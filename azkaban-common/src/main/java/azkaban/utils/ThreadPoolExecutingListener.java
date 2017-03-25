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
package azkaban.utils;

/**
 * Interface for listener to get notified before and after a task has been
 * executed.
 * <pre>
 * 用于在线程池中，开始执行和结束执行的时候do something。
 * Azkaban默认实现：FlowRunnerManager和NoOpThreadPoolExecutingListener
 * </pre>
 * @author hluu
 * 
 */
public interface ThreadPoolExecutingListener {
  public void beforeExecute(Runnable r);

  public void afterExecute(Runnable r);
}