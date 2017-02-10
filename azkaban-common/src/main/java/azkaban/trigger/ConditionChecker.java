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

import java.util.Map;

/**
 * 条件检查器
 */
public interface ConditionChecker {

    /**
     * 检查是否满足条件，如果返回的是true将会触发相应的action
     * @return
     */
  Object eval();

  Object getNum();

    /**
     * 重置条件
     */
  void reset();

    /**
     * 获取检查器的id
     * @return
     */
  String getId();

    /**
     * 获取检查器的类型
     * @return
     */
  String getType();

  ConditionChecker fromJson(Object obj) throws Exception;

  Object toJson();

  void stopChecker();

  void setContext(Map<String, Object> context);

  long getNextCheckTime();

}
