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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import azkaban.utils.Props;
import azkaban.utils.Utils;

/**
 * trigger条件检查器的类型加载器接口，加载并存放 检查器类型名称以及检查器类对应关系列表
 */
public class CheckerTypeLoader {

  private static Logger logger = Logger.getLogger(CheckerTypeLoader.class);

  public static final String DEFAULT_CONDITION_CHECKER_PLUGIN_DIR =
      "plugins/conditioncheckers";

    /** 检查器类型名称以及检查器类对应关系列表*/
  protected static Map<String, Class<? extends ConditionChecker>> checkerToClass =
      new HashMap<String, Class<? extends ConditionChecker>>();

  public void init(Props props) throws TriggerException {
  }

    /**
     * 注册检查器类型，在列表中追加新的检查器类型
     * @param type
     * @param checkerClass
     */
  public synchronized void registerCheckerType(String type,
      Class<? extends ConditionChecker> checkerClass) {
    logger.info("Registering checker " + type);
    if (!checkerToClass.containsKey(type)) {
      checkerToClass.put(type, checkerClass);
    }
  }

  public static void registerBuiltinCheckers(
      Map<String, Class<? extends ConditionChecker>> builtinCheckers) {
    checkerToClass.putAll(builtinCheckers);
    for (String type : builtinCheckers.keySet()) {
      logger.info("Loaded " + type + " checker.");
    }
  }

  public ConditionChecker createCheckerFromJson(String type, Object obj)
      throws Exception {
    ConditionChecker checker = null;
    Class<? extends ConditionChecker> checkerClass = checkerToClass.get(type);
    if (checkerClass == null) {
      throw new Exception("Checker type " + type + " not supported!");
    }
    checker =
        (ConditionChecker) Utils.invokeStaticMethod(
            checkerClass.getClassLoader(), checkerClass.getName(),
            "createFromJson", obj);

    return checker;
  }

    /**
     * 创建{@link ConditionChecker}实例，根据type和构造函数的参数实例化具体的checker（反射）
     * @param type
     * @param args
     * @return
     */
  public ConditionChecker createChecker(String type, Object... args) {
    ConditionChecker checker = null;
    Class<? extends ConditionChecker> checkerClass = checkerToClass.get(type);
    checker = (ConditionChecker) Utils.callConstructor(checkerClass, args);

    return checker;
  }

  public Map<String, Class<? extends ConditionChecker>> getSupportedCheckers() {
    return checkerToClass;
  }

}
