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

package azkaban.execapp.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import azkaban.executor.ExecutableFlow;
import azkaban.executor.ExecutableNode;
import azkaban.executor.Status;

/**
 * <pre>
 * 任务流运行过程中的监听器，
 * 每个任务（普通任务或内嵌任务流）对应一个阻塞状态
 * 本类主要通过维护一个这样的阻塞列表，并提供对任务运行状态的修改，
 * 解锁所有任务的阻塞状态
 * 当状态处于完成状态种类中的任何一个的时候,将解锁当前阻塞状态，并使用Object的notifyAll
 *</pre>
 */
public abstract class FlowWatcher {
  private Logger logger;

  private int execId;
  private ExecutableFlow flow;
  private Map<String, BlockingStatus> map =
      new ConcurrentHashMap<String, BlockingStatus>();
  private boolean cancelWatch = false;

  public FlowWatcher(int execId) {
    this.execId = execId;
  }

  public void setFlow(ExecutableFlow flow) {
    this.flow = flow;
  }

  public void setLogger(Logger logger) {
    this.logger = logger;
  }

  protected Logger getLogger() {
    return this.logger;
  }

  /**
   * Called to fire events to the JobRunner listeners
   *
   * @param jobId
   */
  protected synchronized void handleJobStatusChange(String jobId, Status status) {
    BlockingStatus block = map.get(jobId);
    if (block != null) {
      block.changeStatus(status);
    }
  }

  public int getExecId() {
    return execId;
  }

  public synchronized BlockingStatus getBlockingStatus(String jobId) {
    if (cancelWatch) {
      return null;
    }

    ExecutableNode node = flow.getExecutableNodePath(jobId);
    if (node == null) {
      return null;
    }

    BlockingStatus blockingStatus = map.get(jobId);
    if (blockingStatus == null) {
      blockingStatus = new BlockingStatus(execId, jobId, node.getStatus());
      map.put(jobId, blockingStatus);
    }

    return blockingStatus;
  }

  public Status peekStatus(String jobId) {
    ExecutableNode node = flow.getExecutableNodePath(jobId);
    if (node != null) {
      return node.getStatus();
    }

    return null;
  }

  public synchronized void unblockAllWatches() {
    logger.info("Unblock all watches on " + execId);
    cancelWatch = true;

    for (BlockingStatus status : map.values()) {
      logger.info("Unblocking " + status.getJobId());
      status.changeStatus(Status.SKIPPED);
      status.unblock();
    }

    logger.info("Successfully unblocked all watches on " + execId);
  }

  public boolean isWatchCancelled() {
    return cancelWatch;
  }

    /**
     * 停止任务流的监听
     */
  public abstract void stopWatcher();
}
