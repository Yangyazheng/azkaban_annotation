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

package azkaban.trigger;

import azkaban.utils.Props;

/**
 * 触发器代理，主要用于触发器的管理（利用{@link azkaban.scheduler.ScheduleLoader}将所有的调度计划加载到调度计划列表中，并不断更新和维护这个列表，保证列表中的所有调度计划都处于非过期的状态）
 * ，更新每个Schedule的下次执行时间
 */
public interface TriggerAgent {
  public void loadTriggerFromProps(Props props) throws Exception;

  public String getTriggerSource();

  public void start() throws Exception;

  public void shutdown();

}
