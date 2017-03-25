/*
 * Copyright 2012 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package azkaban.execapp;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import azkaban.event.EventListener;
import azkaban.project.ProjectLoader;
import org.apache.log4j.Appender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;

import azkaban.event.Event;
import azkaban.event.Event.Type;
import azkaban.event.EventHandler;
import azkaban.execapp.event.BlockingStatus;
import azkaban.execapp.event.FlowWatcher;
import azkaban.executor.ExecutableFlowBase;
import azkaban.executor.ExecutableNode;
import azkaban.executor.ExecutorLoader;
import azkaban.executor.ExecutorManagerException;
import azkaban.executor.Status;
import azkaban.flow.CommonJobProperties;
import azkaban.jobExecutor.AbstractProcessJob;
import azkaban.jobExecutor.JavaProcessJob;
import azkaban.jobExecutor.Job;
import azkaban.jobtype.JobTypeManager;
import azkaban.jobtype.JobTypeManagerException;
import azkaban.utils.Props;
import azkaban.utils.StringUtils;

/**
 * 任务流中普通任务的一次运行。
 * <br>
 * note：
 * <ul>
 *     <li>这里的日志使用的是所在流的日志，用于记录和跟踪一整套这个流执行过程中的所有输出信息</li>
 *     <li>每个任务在其内部要使用这个logger时，会加上一个对象锁，内部打印日志的时候不能交叉使用</li>
 * </ul>
 */
public class JobRunner extends EventHandler implements Runnable {
  public static final String AZKABAN_WEBSERVER_URL = "azkaban.webserver.url";

  private final Layout DEFAULT_LAYOUT = new EnhancedPatternLayout(
      "%d{dd-MM-yyyy HH:mm:ss z} %c{1} %p - %m\n");

  private ExecutorLoader loader;
  private Props props;
  private ExecutableNode node;
  private File workingDir;

  private Logger logger = null;
  private Layout loggerLayout = DEFAULT_LAYOUT;
  /** 这里的日志使用的是所在流的日志，用于记录和跟踪一整套这个流执行过程中的所有输出信息*/
  private Logger flowLogger = null;

  private Appender jobAppender;
  private File logFile;
  private String attachmentFileName;

  private Job job;
  private int executionId = -1;
  private String jobId;

  /** 使用所在流的logger时避免冲突使用的对象锁，注意这里使用的是static，也就是会在所有任务之间使用日志的时候会加上锁*/
  private static final Object logCreatorLock = new Object();
  private Object syncObject = new Object();

  private final JobTypeManager jobtypeManager;

  // Used by the job to watch and block against another flow
  private Integer pipelineLevel = null;
  private FlowWatcher watcher = null;
    /** 假设所在外层任务流为flowP，那么pipelineJobs是用于阻塞flowP中当前job所在节点所在的路径*/
  private Set<String> pipelineJobs = new HashSet<String>();

  private Set<String> proxyUsers = null;

    /** 当前任务日志的大小，默认最大5M，超过将截取5M
     * @see FlowRunnerManager#FlowRunnerManager
     * */
  private String jobLogChunkSize;
    /** 日志备份数量，默认为4*/
  private int jobLogBackupIndex;

    /** 延迟开始时间*/
  private long delayStartMs = 0;
  private boolean killed = false;
    /** 当前任务对应的阻塞状态，只是用于当前job runner*/
  private BlockingStatus currentBlockStatus = null;

  public JobRunner(ExecutableNode node, File workingDir, ExecutorLoader loader,
      JobTypeManager jobtypeManager) {
    this.props = node.getInputProps();//当前节点的属性是入口属性
    this.node = node;
    this.workingDir = workingDir;

    this.executionId = node.getParentFlow().getExecutionId();
    this.jobId = node.getId();
    this.loader = loader;
    this.jobtypeManager = jobtypeManager;
  }

  public void setValidatedProxyUsers(Set<String> proxyUsers) {
    this.proxyUsers = proxyUsers;
  }

  public void setLogSettings(Logger flowLogger, String logFileChuckSize,
      int numLogBackup) {
    this.flowLogger = flowLogger;
    this.jobLogChunkSize = logFileChuckSize;
    this.jobLogBackupIndex = numLogBackup;
  }

  public Props getProps() {
    return props;
  }

    /**
     * 形成串行执行的列表。
     * 这个函数用于获取原本可以并行执行的任务列表。
     * @param watcher
     * @param pipelineLevel
     */
  public void setPipeline(FlowWatcher watcher, int pipelineLevel) {
    this.watcher = watcher;
    this.pipelineLevel = pipelineLevel;

    if (this.pipelineLevel == 1) {
      pipelineJobs.add(node.getNestedId());
    } else if (this.pipelineLevel == 2) {
      pipelineJobs.add(node.getNestedId());
      ExecutableFlowBase parentFlow = node.getParentFlow();

      if (parentFlow.getEndNodes().contains(node.getId())) {//是所在内嵌流的尾部节点之一
        if (!parentFlow.getOutNodes().isEmpty()) {
          ExecutableFlowBase grandParentFlow = parentFlow.getParentFlow();
          for (String outNode : parentFlow.getOutNodes()) {
            ExecutableNode nextNode =
                grandParentFlow.getExecutableNode(outNode);

              /**
               * 将后续普通节点加入pipelineJobs中，或者后续内嵌任务流的开始节点加入pipelineJobs中
               */
            // If the next node is a nested flow, then we add the nested
            // starting nodes
            if (nextNode instanceof ExecutableFlowBase) {
              ExecutableFlowBase nextFlow = (ExecutableFlowBase) nextNode;
              findAllStartingNodes(nextFlow, pipelineJobs);
            } else {
              pipelineJobs.add(nextNode.getNestedId());
            }
          }
        }
      } else {//非内嵌流尾部的节点
        for (String outNode : node.getOutNodes()) {
          ExecutableNode nextNode = parentFlow.getExecutableNode(outNode);

          // If the next node is a nested flow, then we add the nested starting
          // nodes
          if (nextNode instanceof ExecutableFlowBase) {
            ExecutableFlowBase nextFlow = (ExecutableFlowBase) nextNode;
            findAllStartingNodes(nextFlow, pipelineJobs);
          } else {
            pipelineJobs.add(nextNode.getNestedId());
          }
        }
      }
    }
  }

    /**
     * 获取所有开始节点。
     * 对于有任务流的嵌套的情况，将递归进行下去，
     * 每一次都取当前任务流节点的开始任务，
     * 如果开始任务是普通任务，直接加入pipelineJobs中；
     * 如果开始任务是内嵌的任务流，那么递归获取这个任务流的开始任务
     * @param flow
     * @param pipelineJobs
     */
  private void findAllStartingNodes(ExecutableFlowBase flow,
      Set<String> pipelineJobs) {
    for (String startingNode : flow.getStartNodes()) {
      ExecutableNode node = flow.getExecutableNode(startingNode);
      if (node instanceof ExecutableFlowBase) {
        findAllStartingNodes((ExecutableFlowBase) node, pipelineJobs);
      } else {
        pipelineJobs.add(node.getNestedId());
      }
    }
  }

  /**
   * Returns a list of jobs that this JobRunner will wait upon to finish before
   * starting. It is only relevant if pipeline is turned on.
   *
   * @return
   */
  public Set<String> getPipelineWatchedJobs() {
    return pipelineJobs;
  }

  public void setDelayStart(long delayMS) {
    delayStartMs = delayMS;
  }

  public long getDelayStart() {
    return delayStartMs;
  }

  public ExecutableNode getNode() {
    return node;
  }

  public String getLogFilePath() {
    return logFile == null ? null : logFile.getPath();
  }

  private void createLogger() {
    // Create logger
    synchronized (logCreatorLock) {
      String loggerName =
          System.currentTimeMillis() + "." + this.executionId + "."
              + this.jobId;
      logger = Logger.getLogger(loggerName);

      // Create file appender
      String logName = createLogFileName(node);
      logFile = new File(workingDir, logName);

      String absolutePath = logFile.getAbsolutePath();

      jobAppender = null;
      try {
        RollingFileAppender fileAppender =
            new RollingFileAppender(loggerLayout, absolutePath, true);
        fileAppender.setMaxBackupIndex(jobLogBackupIndex);
        fileAppender.setMaxFileSize(jobLogChunkSize);
        jobAppender = fileAppender;
        logger.addAppender(jobAppender);
        logger.setAdditivity(false);
      } catch (IOException e) {
        flowLogger.error("Could not open log file in " + workingDir
            + " for job " + this.jobId, e);
      }
    }
  }

  private void createAttachmentFile() {
    String fileName = createAttachmentFileName(node);
    File file = new File(workingDir, fileName);
    attachmentFileName = file.getAbsolutePath();
  }

  private void closeLogger() {
    if (jobAppender != null) {
      logger.removeAppender(jobAppender);
      jobAppender.close();
    }
  }

  private void writeStatus() {
    try {
      node.setUpdateTime(System.currentTimeMillis());
      loader.updateExecutableNode(node);
    } catch (ExecutorManagerException e) {
      flowLogger.error("Could not update job properties in db for "
          + this.jobId, e);
    }
  }

  /**
   * Used to handle non-ready and special status's (i.e. KILLED). Returns true
   * if they handled anything.
   * 处理任务不需要运行的情况--更正：处理当轮到这个任务实际运行的时候，下一个状态不是READY的情况
   * @return
   */
  private boolean handleNonReadyStatus() {
    Status nodeStatus = node.getStatus();
    boolean quickFinish = false;
    long time = System.currentTimeMillis();

    if (Status.isStatusFinished(nodeStatus)) {
      quickFinish = true;
    } else if (nodeStatus == Status.DISABLED) {//禁用
      changeStatus(Status.SKIPPED, time);
      quickFinish = true;
    } else if (this.isKilled()) {
      changeStatus(Status.KILLED, time);
      quickFinish = true;
    }

    if (quickFinish) {
      node.setStartTime(time);
      fireEvent(Event.create(this, Type.JOB_STARTED, null, false));
      node.setEndTime(time);
      fireEvent(Event.create(this, Type.JOB_FINISHED));
      return true;
    }

    return false;
  }

  /**
   * If pipelining is set, will block on another flow's jobs.
   * return true if something went wrong
   */
  private boolean blockOnPipeLine() {
    if (this.isKilled()) {
      return true;
    }

    // For pipelining of jobs. Will watch other jobs.
    if (!pipelineJobs.isEmpty()) {
      String blockedList = "";
      ArrayList<BlockingStatus> blockingStatus =
          new ArrayList<BlockingStatus>();
      for (String waitingJobId : pipelineJobs) {
        Status status = watcher.peekStatus(waitingJobId);
        if (status != null && !Status.isStatusFinished(status)) {
          BlockingStatus block = watcher.getBlockingStatus(waitingJobId);
          blockingStatus.add(block);
          blockedList += waitingJobId + ",";
        }
      }
      if (!blockingStatus.isEmpty()) {
        logger.info("Pipeline job " + this.jobId + " waiting on " + blockedList
            + " in execution " + watcher.getExecId());

          /** 遍历阻塞状态列表，一个一个调用阻塞状态的等待执行成功方法，
           * 不会浪费很多时间，总的等待时间差不多是这些阻塞状态里面状态变为完成所用时间的最大值
           * */
        for (BlockingStatus bStatus : blockingStatus) {
          logger.info("Waiting on pipelined job " + bStatus.getJobId());
          currentBlockStatus = bStatus;
          bStatus.blockOnFinishedStatus();
          if (this.isKilled()) {
            logger.info("Job was killed while waiting on pipeline. Quiting.");
            return true;
          } else {
            logger.info("Pipelined job " + bStatus.getJobId() + " finished.");
          }
        }
      }
    }

    currentBlockStatus = null;
    return false;
  }

    /**
     * return true if something went wrong
     * @return
     */
  private boolean delayExecution() {
    if (this.isKilled()) {
      return true;
    }

    long currentTime = System.currentTimeMillis();
    if (delayStartMs > 0) {
      logger.info("Delaying start of execution for " + delayStartMs
          + " milliseconds.");
      synchronized (this) {
        try {
          this.wait(delayStartMs);
          logger.info("Execution has been delayed for " + delayStartMs
              + " ms. Continuing with execution.");
        } catch (InterruptedException e) {
          logger.error("Job " + this.jobId + " was to be delayed for "
              + delayStartMs + ". Interrupted after "
              + (System.currentTimeMillis() - currentTime));
        }
      }

      if (this.isKilled()) {
        logger.info("Job was killed while in delay. Quiting.");
        return true;
      }
    }

    return false;
  }

  private void finalizeLogFile(int attemptNo) {
    closeLogger();
    if (logFile == null) {
      flowLogger.info("Log file for job " + this.jobId + " is null");
      return;
    }

    try {
      File[] files = logFile.getParentFile().listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.startsWith(logFile.getName());
        }
      });
      Arrays.sort(files, Collections.reverseOrder());

      loader.uploadLogFile(executionId, this.node.getNestedId(), attemptNo,
          files);
    } catch (ExecutorManagerException e) {
      flowLogger.error(
          "Error writing out logs for job " + this.node.getNestedId(), e);
    }
  }

  private void finalizeAttachmentFile() {
    if (attachmentFileName == null) {
      flowLogger.info("Attachment file for job " + this.jobId + " is null");
      return;
    }

    try {
      File file = new File(attachmentFileName);
      if (!file.exists()) {
        flowLogger.info("No attachment file for job " + this.jobId
            + " written.");
        return;
      }
      loader.uploadAttachmentFile(node, file);
    } catch (ExecutorManagerException e) {
      flowLogger.error(
          "Error writing out attachment for job " + this.node.getNestedId(), e);
    }
  }

  /**
   * The main run thread.
   */
  @Override
  public void run() {
    Thread.currentThread().setName(
        "JobRunner-" + this.jobId + "-" + executionId);

    // If the job is cancelled, disabled, killed. No log is created in this case
    if (handleNonReadyStatus()) {//处理任务不需要运行的情况，如：任务取消
      return;
    }

    createAttachmentFile();
    createLogger();
    boolean errorFound = false;
    // Delay execution if necessary. Will return a true if something went wrong.
    errorFound |= delayExecution();

    // For pipelining of jobs. Will watch other jobs. Will return true if
    // something went wrong.
    errorFound |= blockOnPipeLine();

    // Start the node.
    node.setStartTime(System.currentTimeMillis());
    if (!errorFound && !isKilled()) {
      fireEvent(Event.create(this, Type.JOB_STARTED, null, false));
      try {
          /** 上传可执行普通任务，默认实现是在数据库表execution_jobs中记录一条信息，*/
        loader.uploadExecutableNode(node, props);
      } catch (ExecutorManagerException e1) {
        logger.error("Error writing initial node properties");
      }

      if (prepareJob()) {
        // Writes status to the db
        writeStatus();
        fireEvent(Event.create(this, Type.JOB_STATUS_CHANGED), false);
        runJob();
      } else {
        changeStatus(Status.FAILED);
        logError("Job run failed preparing the job.");
      }
    }
    node.setEndTime(System.currentTimeMillis());

    if (isKilled()) {
      // even if it's killed, there is a chance that the job failed is marked as
      // failure,
      // So we set it to KILLED to make sure we know that we forced kill it
      // rather than
      // it being a legitimate failure.
      changeStatus(Status.KILLED);
    }

    int attemptNo = node.getAttempt();
    logInfo("Finishing job " + this.jobId + " attempt: " + attemptNo + " at "
        + node.getEndTime() + " with status " + node.getStatus());

    fireEvent(Event.create(this, Type.JOB_FINISHED), false);
    finalizeLogFile(attemptNo);
    finalizeAttachmentFile();
    writeStatus();
  }

  private boolean prepareJob() throws RuntimeException {
    // Check pre conditions
    if (props == null || this.isKilled()) {
      logError("Failing job. The job properties don't exist");
      return false;
    }

    synchronized (syncObject) {
      if (node.getStatus() == Status.FAILED || this.isKilled()) {
        return false;
      }

      if (node.getAttempt() > 0) {
        logInfo("Starting job " + this.jobId + " attempt " + node.getAttempt()
            + " at " + node.getStartTime());
      } else {
        logInfo("Starting job " + this.jobId + " at " + node.getStartTime());
      }

      // If it's an embedded flow, we'll add the nested flow info to the job
      // conf
      if (node.getExecutableFlow() != node.getParentFlow()) {
        String subFlow = node.getPrintableId(":");
        props.put(CommonJobProperties.NESTED_FLOW_PATH, subFlow);
      }

      insertJobMetadata();
      insertJVMAargs();

      props.put(CommonJobProperties.JOB_ATTEMPT, node.getAttempt());
      props.put(CommonJobProperties.JOB_METADATA_FILE,
          createMetaDataFileName(node));
      props.put(CommonJobProperties.JOB_ATTACHMENT_FILE, attachmentFileName);
      changeStatus(Status.RUNNING);

      // Ability to specify working directory
      if (!props.containsKey(AbstractProcessJob.WORKING_DIR)) {
        props.put(AbstractProcessJob.WORKING_DIR, workingDir.getAbsolutePath());
      }

      if (props.containsKey("user.to.proxy")) {
        String jobProxyUser = props.getString("user.to.proxy");
        if (proxyUsers != null && !proxyUsers.contains(jobProxyUser)) {
          logger.error("User " + jobProxyUser
              + " has no permission to execute this job " + this.jobId + "!");
          return false;
        }
      }

      try {
        job = jobtypeManager.buildJobExecutor(this.jobId, props, logger);
      } catch (JobTypeManagerException e) {
        logger.error("Failed to build job type", e);
        return false;
      }
    }

    return true;
  }

  /**
   * Add useful JVM arguments so it is easier to map a running Java process to a
   * flow, execution id and job
   */
  private void insertJVMAargs() {
    String flowName = node.getParentFlow().getFlowId();
    String jobId = node.getId();

    String jobJVMArgs =
        String.format(
            "-Dazkaban.flowid=%s -Dazkaban.execid=%s -Dazkaban.jobid=%s",
            flowName, executionId, jobId);

    String previousJVMArgs = props.get(JavaProcessJob.JVM_PARAMS);
    jobJVMArgs += (previousJVMArgs == null) ? "" : " " + previousJVMArgs;

    logger.info("job JVM args: " + jobJVMArgs);
    props.put(JavaProcessJob.JVM_PARAMS, jobJVMArgs);
  }

  /**
   * Add relevant links to the job properties so that downstream consumers may
   * know what executions initiated their execution.
   */
  private void insertJobMetadata() {
    Props azkProps = AzkabanExecutorServer.getApp().getAzkabanProps();
    String baseURL = azkProps.get(AZKABAN_WEBSERVER_URL);
    if (baseURL != null) {
      String flowName = node.getParentFlow().getFlowId();
      String projectName = node.getParentFlow().getProjectName();

      props.put(CommonJobProperties.AZKABAN_URL, baseURL);
      props.put(CommonJobProperties.EXECUTION_LINK,
          String.format("%s/executor?execid=%d", baseURL, executionId));
      props.put(CommonJobProperties.JOBEXEC_LINK, String.format(
          "%s/executor?execid=%d&job=%s", baseURL, executionId, jobId));
      props.put(CommonJobProperties.ATTEMPT_LINK, String.format(
          "%s/executor?execid=%d&job=%s&attempt=%d", baseURL, executionId,
          jobId, node.getAttempt()));
      props.put(CommonJobProperties.WORKFLOW_LINK, String.format(
          "%s/manager?project=%s&flow=%s", baseURL, projectName, flowName));
      props.put(CommonJobProperties.JOB_LINK, String.format(
          "%s/manager?project=%s&flow=%s&job=%s", baseURL, projectName,
          flowName, jobId));
    } else {
      if (logger != null) {
        logger.info(AZKABAN_WEBSERVER_URL + " property was not set");
      }
    }
    // out nodes
    props.put(CommonJobProperties.OUT_NODES,
        StringUtils.join2(node.getOutNodes(), ","));

    // in nodes
    props.put(CommonJobProperties.IN_NODES,
        StringUtils.join2(node.getInNodes(), ","));
  }

  private void runJob() {
    try {
      job.run();
    } catch (Throwable e) {

      if (props.getBoolean("job.succeed.on.failure", false)) {
        changeStatus(Status.FAILED_SUCCEEDED);
        logError("Job run failed, but will treat it like success.");
        logError(e.getMessage() + " cause: " + e.getCause(), e);
      } else {
        changeStatus(Status.FAILED);
        logError("Job run failed!", e);
        logError(e.getMessage() + " cause: " + e.getCause());
      }
    }

    if (job != null) {
      node.setOutputProps(job.getJobGeneratedProperties());
    }

    // If the job is still running, set the status to Success.
    if (!Status.isStatusFinished(node.getStatus())) {
      changeStatus(Status.SUCCEEDED);
    }
  }

  private void changeStatus(Status status) {
    changeStatus(status, System.currentTimeMillis());
  }

  private void changeStatus(Status status, long time) {
    node.setStatus(status);
    node.setUpdateTime(time);
  }

  private void fireEvent(Event event) {
    fireEvent(event, true);
  }

    /**
     * 用于调用{@link EventHandler#fireEventListeners}，让它遍历所有监听器队列，
     * 调用所有监听器的{@link EventListener#handleEvent(Event)},从而达到让感兴趣的监听器处理当前事件
     * @param event
     * @param updateTime
     */
  private void fireEvent(Event event, boolean updateTime) {
    if (updateTime) {
      node.setUpdateTime(System.currentTimeMillis());
    }
    this.fireEventListeners(event);
  }

  public void kill() {
    synchronized (syncObject) {
      if (Status.isStatusFinished(node.getStatus())) {
        return;
      }
      logError("Kill has been called.");
      this.killed = true;

      BlockingStatus status = currentBlockStatus;
      if (status != null) {
        status.unblock();
      }

      // Cancel code here
      if (job == null) {
        logError("Job hasn't started yet.");
        // Just in case we're waiting on the delay
        synchronized (this) {
          this.notify();
        }
        return;
      }

      try {
        job.cancel();
      } catch (Exception e) {
        logError(e.getMessage());
        logError("Failed trying to cancel job. Maybe it hasn't started running yet or just finished.");
      }

      this.changeStatus(Status.KILLED);
    }
  }

  public boolean isKilled() {
    return killed;
  }

  public Status getStatus() {
    return node.getStatus();
  }

  private void logError(String message) {
    if (logger != null) {
      logger.error(message);
    }
  }

  private void logError(String message, Throwable t) {
    if (logger != null) {
      logger.error(message, t);
    }
  }

  private void logInfo(String message) {
    if (logger != null) {
      logger.info(message);
    }
  }

  public File getLogFile() {
    return logFile;
  }

  public Logger getLogger() {
    return logger;
  }

  public static String createLogFileName(ExecutableNode node, int attempt) {
    int executionId = node.getExecutableFlow().getExecutionId();
    String jobId = node.getId();
    if (node.getExecutableFlow() != node.getParentFlow()) {
      // Posix safe file delimiter
      jobId = node.getPrintableId("._.");
    }
    return attempt > 0 ? "_job." + executionId + "." + attempt + "." + jobId
        + ".log" : "_job." + executionId + "." + jobId + ".log";
  }

  public static String createLogFileName(ExecutableNode node) {
    return JobRunner.createLogFileName(node, node.getAttempt());
  }

  public static String createMetaDataFileName(ExecutableNode node, int attempt) {
    int executionId = node.getExecutableFlow().getExecutionId();
    String jobId = node.getId();
    if (node.getExecutableFlow() != node.getParentFlow()) {
      // Posix safe file delimiter
      jobId = node.getPrintableId("._.");
    }

    return attempt > 0 ? "_job." + executionId + "." + attempt + "." + jobId
        + ".meta" : "_job." + executionId + "." + jobId + ".meta";
  }

  public static String createMetaDataFileName(ExecutableNode node) {
    return JobRunner.createMetaDataFileName(node, node.getAttempt());
  }

  public static String createAttachmentFileName(ExecutableNode node) {

    return JobRunner.createAttachmentFileName(node, node.getAttempt());
  }

  public static String createAttachmentFileName(ExecutableNode node, int attempt) {
    int executionId = node.getExecutableFlow().getExecutionId();
    String jobId = node.getId();
    if (node.getExecutableFlow() != node.getParentFlow()) {
      // Posix safe file delimiter
      jobId = node.getPrintableId("._.");
    }

    return attempt > 0 ? "_job." + executionId + "." + attempt + "." + jobId
        + ".attach" : "_job." + executionId + "." + jobId + ".attach";
  }
}