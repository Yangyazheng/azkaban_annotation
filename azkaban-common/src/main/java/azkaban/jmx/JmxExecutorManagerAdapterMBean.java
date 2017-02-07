/*
 * Copyright 2014 LinkedIn Corp.
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

package azkaban.jmx;

import java.util.List;

public interface JmxExecutorManagerAdapterMBean {
    /**
     * 获取运行中的任务流数量
     * @return
     */
  @DisplayName("OPERATION: getNumRunningFlows")
  public int getNumRunningFlows();

    /**
     * 获取执行器的线程状态
     * @return
     */
  @DisplayName("OPERATION: getExecutorThreadState")
  public String getExecutorManagerThreadState();

    /**
     * 执行器线程是否处于活跃状态
     * @return
     */
  @DisplayName("OPERATION: isThreadActive")
  public boolean isExecutorManagerThreadActive();

    /**
     * 获取上次检查执行器线程的时间
     * @return
     */
  @DisplayName("OPERATION: getLastThreadCheckTime")
  public Long getLastExecutorManagerThreadCheckTime();

    /**
     * 获取主执行器的IP和端口
     * @return
     */
  @DisplayName("OPERATION: getPrimaryExecutorHostPorts")
  public List<String> getPrimaryExecutorHostPorts();

  // @DisplayName("OPERATION: getExecutorThreadStage")
  // public String getExecutorThreadStage();
  //
  // @DisplayName("OPERATION: getRunningFlows")
  // public String getRunningFlows();
}
