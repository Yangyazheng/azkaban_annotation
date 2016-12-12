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

package azkaban.alert;

import azkaban.executor.ExecutableFlow;
import azkaban.sla.SlaOption;

/**
 * 任务流执行结果提示接口
 */
public interface Alerter {
    /**
     * 任务流执行成功提示
     * @param exflow 执行流
     * @throws Exception
     */
  void alertOnSuccess(ExecutableFlow exflow) throws Exception;

    /**
     * 任务流执行错误提示
     * @param exflow
     * @param extraReasons 出错原因，可以是多个String类型的多参数
     * @throws Exception
     */
  void alertOnError(ExecutableFlow exflow, String ... extraReasons) throws Exception;

    /**
     * 任务流首次执行错误提示
     * @param exflow
     * @throws Exception
     */
  void alertOnFirstError(ExecutableFlow exflow) throws Exception;

    /**
     *
     * @param slaOption
     * @param slaMessage
     * @throws Exception
     */
  void alertOnSla(SlaOption slaOption, String slaMessage) throws Exception;
}
