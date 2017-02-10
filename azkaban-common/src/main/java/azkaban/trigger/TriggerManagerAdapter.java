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

import java.util.List;
import java.util.Map;

/**
 * Trigger管理器适配器接口，用于触发器的管理
 */
public interface TriggerManagerAdapter {//对系统的触发器进行管理

    /**
     * 增加触发器
     * @param t
     * @param user
     * @throws TriggerManagerException
     */
  public void insertTrigger(Trigger t, String user)
      throws TriggerManagerException;

    /**
     * 移除触发器
     * @param id
     * @param user
     * @throws TriggerManagerException
     */
  public void removeTrigger(int id, String user) throws TriggerManagerException;

    /**
     * 更新触发器
     * @param t
     * @param user
     * @throws TriggerManagerException
     */
  public void updateTrigger(Trigger t, String user)
      throws TriggerManagerException;

    /**
     * 获取 自给定参数时间以来更新过的触发器
     * @param lastUpdateTime
     * @return
     * @throws TriggerManagerException
     */
  public List<Trigger> getAllTriggerUpdates(long lastUpdateTime)
      throws TriggerManagerException;

    /**
     * 获取triggerSource为给定参数，自给定参数时间以来更新过的触发器
     * @param triggerSource
     * @param lastUpdateTime
     * @return
     * @throws TriggerManagerException
     */
  public List<Trigger> getTriggerUpdates(String triggerSource,
      long lastUpdateTime) throws TriggerManagerException;

    /**
     * 获取triggerSource为给定参数的触发器
     * @param triggerSource
     * @return
     * @throws TriggerManagerException
     */
  public List<Trigger> getTriggers(String triggerSource);

  public void start() throws TriggerManagerException;

  public void shutdown();

    /**
     * 注册触发器类型
     * @param name
     * @param checker
     */
  public void registerCheckerType(String name,
      Class<? extends ConditionChecker> checker);

    /**
     * 注册触发器动作类型
     * @param name
     * @param action
     */
  public void registerActionType(String name,
      Class<? extends TriggerAction> action);

  public TriggerJMX getJMX();


    /**
     *
     */
    public interface TriggerJMX {
    public long getLastRunnerThreadCheckTime();

    public boolean isRunnerThreadActive();

    public String getPrimaryServerHost();

    public int getNumTriggers();

    public String getTriggerSources();

    public String getTriggerIds();

    public long getScannerIdleTime();

    public Map<String, Object> getAllJMXMbeans();

    public String getScannerThreadStage();
  }

}
