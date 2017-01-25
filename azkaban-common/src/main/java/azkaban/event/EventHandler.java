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

import java.util.ArrayList;
import java.util.HashSet;

/**
 * azkaban event处理器，
 * 通过将注册的事件放入列表中，每次遍历列表轮询，进行事件处理
 */
public class EventHandler {
  private HashSet<EventListener> listeners = new HashSet<EventListener>();

  public EventHandler() {
  }

  public void addListener(EventListener listener) {
    listeners.add(listener);
  }

    /**
     * 触发事件监听器，遍历整个注册事件列表，调用每个监听器的处理事件方法
     * @param event
     */
  public void fireEventListeners(Event event) {
    ArrayList<EventListener> listeners =
        new ArrayList<EventListener>(this.listeners);
    for (EventListener listener : listeners) {
      listener.handleEvent(event);
    }
  }

    /**
     * 移除事件监听
     * @param listener
     */
  public void removeListener(EventListener listener) {
    listeners.remove(listener);
  }
}
