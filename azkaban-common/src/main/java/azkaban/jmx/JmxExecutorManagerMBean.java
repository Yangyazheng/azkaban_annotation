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

public interface JmxExecutorManagerMBean {
    /**
     * 获取运行中的任务流数量
     * @return
     */
  @DisplayName("OPERATION: getNumRunningFlows")
  public int getNumRunningFlows();

    /**
     * 获取运行中的任务流信息
     * @return
     */
  @DisplayName("OPERATION: getRunningFlows")
  public String getRunningFlows();

    /**
     * 获取执行器的线程状态
     * @return
     */
  @DisplayName("OPERATION: getExecutorThreadState")
  public String getExecutorThreadState();

    /**
     * 获取执行器线程阶段
     * @return
     */
  @DisplayName("OPERATION: getExecutorThreadStage")
  public String getExecutorThreadStage();

    /**
     * 执行器线程是否处于活跃状态
     * @return
     */
  @DisplayName("OPERATION: isThreadActive")
  public boolean isThreadActive();

    /**
     * 获取上次检查执行器线程的时间
     * @return
     */
  @DisplayName("OPERATION: getLastThreadCheckTime")
  public Long getLastThreadCheckTime();

    /**
     * 获取主执行器的IP和端口
     * @return
     */
  @DisplayName("OPERATION: getPrimaryExecutorHostPorts")
  public List<String> getPrimaryExecutorHostPorts();

    /**
     * 判断队列处理器是否处于活跃状态
     * @return
     */
  @DisplayName("OPERATION: isQueueProcessorActive")
  public boolean isQueueProcessorActive();

    /**
     * 获取排在队列中的任务流
     * @return
     */
  @DisplayName("OPERATION: getQueuedFlows")
  public String getQueuedFlows();

    /**
     * 获取队列处理器线程的状态
     * @return
     */
  @DisplayName("OPERATION: getQueueProcessorThreadState")
  public String getQueueProcessorThreadState();

    /**
     * 获取可用的执行器比较算子的名称列表
     * @return
     */
  @DisplayName("OPERATION: getAvailableExecutorComparatorNames")
  List<String> getAvailableExecutorComparatorNames();

    /**
     * 获取可用的执行器过滤算子的名称列表
     * @return
     */
  @DisplayName("OPERATION: getAvailableExecutorFilterNames")
  List<String> getAvailableExecutorFilterNames();

  @DisplayName("OPERATION: getLastSuccessfulExecutorInfoRefresh")
  long getLastSuccessfulExecutorInfoRefresh();

}
