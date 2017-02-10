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

package azkaban.scheduler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.joda.time.DateTimeZone;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import azkaban.executor.ExecutionOptions;
import azkaban.sla.SlaOption;
import azkaban.trigger.TriggerAgent;
import azkaban.trigger.TriggerStatus;
import azkaban.utils.Pair;
import azkaban.utils.Props;

/**
 * The ScheduleManager stores and executes the schedule. It uses a single thread
 * instead and waits until correct loading time for the flow. It will not remove
 * the flow from the schedule when it is run, which can potentially allow the
 * flow to and overlap each other.
 * 每一次调用{@link ScheduleManager#insertSchedule}的时候都会更新调度计划列表中对应的任务流的下次执行时间
 */
public class ScheduleManager implements TriggerAgent {
  private static Logger logger = Logger.getLogger(ScheduleManager.class);

  public static final String triggerSource = "SimpleTimeTrigger";
  private final DateTimeFormatter _dateFormat = DateTimeFormat
      .forPattern("MM-dd-yyyy HH:mm:ss:SSS");
  private ScheduleLoader loader;

  private Map<Integer, Schedule> scheduleIDMap =
      new LinkedHashMap<Integer, Schedule>();
  private Map<Pair<Integer, String>, Schedule> scheduleIdentityPairMap =
      new LinkedHashMap<Pair<Integer, String>, Schedule>();

  /**
   * Give the schedule manager a loader class that will properly load the
   * schedule.
   *
   * @param loader
   */
  public ScheduleManager(ScheduleLoader loader) {
    this.loader = loader;
  }

    /**
     * 利用{@link ScheduleLoader}加载调度计划,移除过期计划，将没有过期的计划在本对象中记录跟踪
     * @throws ScheduleManagerException
     */
  @Override
  public void start() throws ScheduleManagerException {
    List<Schedule> scheduleList = null;
    try {
      scheduleList = loader.loadSchedules();
    } catch (ScheduleManagerException e) {
      logger.error("Failed to load schedules" + e.getCause() + e.getMessage());
      e.printStackTrace();
    }

    for (Schedule sched : scheduleList) {
      if (sched.getStatus().equals(TriggerStatus.EXPIRED.toString())) {
        onScheduleExpire(sched);
      } else {
        internalSchedule(sched);
      }
    }

  }

  // only do this when using external runner
  public synchronized void updateLocal() throws ScheduleManagerException {
    List<Schedule> updates = loader.loadUpdatedSchedules();
    for (Schedule s : updates) {
      if (s.getStatus().equals(TriggerStatus.EXPIRED.toString())) {
        onScheduleExpire(s);
      } else {
        internalSchedule(s);
      }
    }
  }

    /**
     * 从本对象的调度计划记录中移除那些过期的计划
     * @param s
     */
  private void onScheduleExpire(Schedule s) {
    removeSchedule(s);
  }

  /**
   * Shutdowns the scheduler thread. After shutdown, it may not be safe to use
   * it again.
   */
  public void shutdown() {

  }

  /**
   * Retrieves a copy of the list of schedules.
   *
   * @return
   * @throws ScheduleManagerException
   */
  public synchronized List<Schedule> getSchedules()
      throws ScheduleManagerException {

    updateLocal();
    return new ArrayList<Schedule>(scheduleIDMap.values());
  }

  /**
   * Returns the scheduled flow for the flow name
   *
   * @throws ScheduleManagerException
   */
  public Schedule getSchedule(int projectId, String flowId)
      throws ScheduleManagerException {
    updateLocal();
    return scheduleIdentityPairMap.get(new Pair<Integer, String>(projectId,
        flowId));
  }

  /**
   * Returns the scheduled flow for the scheduleId
   *
   * @throws ScheduleManagerException
   */
  public Schedule getSchedule(int scheduleId) throws ScheduleManagerException {
    updateLocal();
    return scheduleIDMap.get(scheduleId);
  }

  /**
   * Removes the flow from the schedule if it exists.
   *
   * @throws ScheduleManagerException
   */

  public synchronized void removeSchedule(int projectId, String flowId)
      throws ScheduleManagerException {
    Schedule sched = getSchedule(projectId, flowId);
    if (sched != null) {
      removeSchedule(sched);
    }
  }

  /**
   * 从调度计划列表中移除计划
   * Removes the flow from the schedule if it exists.
   *
   * @param sched
   */
  public synchronized void removeSchedule(Schedule sched) {
    Pair<Integer, String> identityPairMap = sched.getScheduleIdentityPair();

    Schedule schedule = scheduleIdentityPairMap.get(identityPairMap);
    if (schedule != null) {
      scheduleIdentityPairMap.remove(identityPairMap);
    }

    scheduleIDMap.remove(sched.getScheduleId());

    try {
      loader.removeSchedule(sched);
    } catch (ScheduleManagerException e) {
      e.printStackTrace();
    }
  }

    /**
     * 为任务流创建调度计划，并加到计划列表中
     * @param scheduleId
     * @param projectId
     * @param projectName
     * @param flowName
     * @param status
     * @param firstSchedTime
     * @param timezone
     * @param period
     * @param lastModifyTime
     * @param nextExecTime
     * @param submitTime
     * @param submitUser
     * @return
     */
  public Schedule scheduleFlow(final int scheduleId, final int projectId,
      final String projectName, final String flowName, final String status,
      final long firstSchedTime, final DateTimeZone timezone,
      final ReadablePeriod period, final long lastModifyTime,
      final long nextExecTime, final long submitTime, final String submitUser) {
    return scheduleFlow(scheduleId, projectId, projectName, flowName, status,
        firstSchedTime, timezone, period, lastModifyTime, nextExecTime,
        submitTime, submitUser, null, null);
  }

    /**
     * 为任务流创建调度计划，并加到计划列表中
     * @param scheduleId
     * @param projectId
     * @param projectName
     * @param flowName
     * @param status
     * @param firstSchedTime
     * @param timezone
     * @param period
     * @param lastModifyTime
     * @param nextExecTime
     * @param submitTime
     * @param submitUser
     * @param execOptions
     * @param slaOptions
     * @return
     */
  public Schedule scheduleFlow(final int scheduleId, final int projectId,
      final String projectName, final String flowName, final String status,
      final long firstSchedTime, final DateTimeZone timezone,
      final ReadablePeriod period, final long lastModifyTime,
      final long nextExecTime, final long submitTime, final String submitUser,
      ExecutionOptions execOptions, List<SlaOption> slaOptions) {
    Schedule sched =
        new Schedule(scheduleId, projectId, projectName, flowName, status,
            firstSchedTime, timezone, period, lastModifyTime, nextExecTime,
            submitTime, submitUser, execOptions, slaOptions);
    logger
        .info("Scheduling flow '" + sched.getScheduleName() + "' for "
            + _dateFormat.print(firstSchedTime) + " with a period of " + period == null ? "(non-recurring)"
            : period);

    insertSchedule(sched);
    return sched;
  }

  /**
   * Schedules the flow, but doesn't save the schedule afterwards.
   *
   * @param s
   */
  private synchronized void internalSchedule(Schedule s) {
    scheduleIDMap.put(s.getScheduleId(), s);
    scheduleIdentityPairMap.put(s.getScheduleIdentityPair(), s);
  }

  /**
   * Adds a flow to the schedule.
   * 对于已经存在的调度计划，将会重新计算下次执行时间，并更新调度计划列表中的下次执行时间
   *
   * @param s
   */
  public synchronized void insertSchedule(Schedule s) {
    Schedule exist = scheduleIdentityPairMap.get(s.getScheduleIdentityPair());
    if (s.updateTime()) {
      try {
        if (exist == null) {
          loader.insertSchedule(s);
          internalSchedule(s);
        } else {
          s.setScheduleId(exist.getScheduleId());
          loader.updateSchedule(s);
          internalSchedule(s);
        }
      } catch (ScheduleManagerException e) {
        e.printStackTrace();
      }
    } else {
      logger
          .error("The provided schedule is non-recurring and the scheduled time already passed. "
              + s.getScheduleName());
    }
  }

  @Override
  public void loadTriggerFromProps(Props props) throws ScheduleManagerException {
    throw new ScheduleManagerException("create " + getTriggerSource()
        + " from json not supported yet");
  }

  @Override
  public String getTriggerSource() {
    return triggerSource;
  }
}
