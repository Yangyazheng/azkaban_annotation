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

package azkaban.event;

/**
 * 事件监听接口,这个系统中对任务、任务流的监听都是通过这个接口；
 * 这是整个系统中事件监听机制的最抽象的基类
 */
public interface EventListener {
    /**
     * 处理事件
     * @param event
     */
  public void handleEvent(Event event);
}
