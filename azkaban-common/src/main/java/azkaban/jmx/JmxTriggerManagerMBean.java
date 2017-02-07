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

/**
 * JMX触发器管理类接口
 */
public interface JmxTriggerManagerMBean {

    /**
     * 运行线程上一次检查时间
     * @return
     */
  @DisplayName("OPERATION: getLastThreadCheckTime")
  public long getLastRunnerThreadCheckTime();

    /**
     * 判断运行线程是否处于活跃状态
     * @return
     */
  @DisplayName("OPERATION: isThreadActive")
  public boolean isRunnerThreadActive();

    /**
     * 获取主触发器的IP和端口
     * @return
     */
  @DisplayName("OPERATION: getPrimaryTriggerHostPort")
  public String getPrimaryTriggerHostPort();

  // @DisplayName("OPERATION: getAllTriggerHostPorts")
  // public List<String> getAllTriggerHostPorts();

    /**
     * 获取触发器的数量
     * @return
     */
  @DisplayName("OPERATION: getNumTriggers")
  public int getNumTriggers();

    /**
     * 获取触发器的资源
     * @return
     */
  @DisplayName("OPERATION: getTriggerSources")
  public String getTriggerSources();

    /**
     * 获取触发器的id
     * @return
     */
  @DisplayName("OPERATION: getTriggerIds")
  public String getTriggerIds();

    /**
     * 获取扫描器的扫描间隔时间
     * @return
     */
  @DisplayName("OPERATION: getScannerIdleTime")
  public long getScannerIdleTime();

    /**
     * 获取扫描线程的阶段
     * @return
     */
  @DisplayName("OPERATION: getScannerThreadStage")
  public String getScannerThreadStage();
}
