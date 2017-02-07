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
 * jetty server 管理接口
 */
public interface JmxJettyServerMBean {
    /**
     * jetty server LifeCycle: is server running
     * @return
     */
  @DisplayName("OPERATION: isRunning")
  public boolean isRunning();

    /**
     * jetty server LifeCycle: is server failed
     * @return
     */
  @DisplayName("OPERATION: isFailed")
  public boolean isFailed();

    /**
     * jetty server LifeCycle: is server stopped
     * @return
     */
  @DisplayName("OPERATION: isStopped")
  public boolean isStopped();

    /**
     * get the num of running thread,线程池中线程的数量
     * @return
     */
  @DisplayName("OPERATION: getNumThreads")
  public int getNumThreads();

  @DisplayName("OPERATION: getNumIdleThreads")
  public int getNumIdleThreads();

  @DisplayName("OPERATION: getHost")
  public String getHost();

  @DisplayName("OPERATION: getPort")
  public int getPort();

  @DisplayName("OPERATION: getConfidentialPort")
  public int getConfidentialPort();

  @DisplayName("OPERATION: getConnections")
  public int getConnections();

    /**
     * 获取打开的连接数量
     * @return
     */
  @DisplayName("OPERATION: getConnectionsOpen")
  public int getConnectionsOpen();

    /**
     * 获取最大连接数
     * @return
     */
  @DisplayName("OPERATION: getConnectionsOpenMax")
  public int getConnectionsOpenMax();

    /**
     * 获取最小连接数
     * @return
     */
  @DisplayName("OPERATION: getConnectionsOpenMin")
  public int getConnectionsOpenMin();

  @DisplayName("OPERATION: getConnectionsDurationAve")
  public long getConnectionsDurationAve();

  @DisplayName("OPERATION: getConnectionsDurationMax")
  public long getConnectionsDurationMax();

  @DisplayName("OPERATION: getConnectionsDurationMin")
  public long getConnectionsDurationMin();

  @DisplayName("OPERATION: getConnectionsDurationTotal")
  public long getConnectionsDurationTotal();

  @DisplayName("OPERATION: getConnectionsRequestAve")
  public long getConnectionsRequestAve();

  @DisplayName("OPERATION: getConnectionsRequestMax")
  public long getConnectionsRequestMax();

  @DisplayName("OPERATION: getConnectionsRequestMin")
  public long getConnectionsRequestMin();

  @DisplayName("OPERATION: turnStatsOn")
  public void turnStatsOn();

  @DisplayName("OPERATION: turnStatsOff")
  public void turnStatsOff();

  @DisplayName("OPERATION: resetStats")
  public void resetStats();

  @DisplayName("OPERATION: isStatsOn")
  public boolean isStatsOn();
}
