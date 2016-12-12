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

package azkaban.executor.mail;

import azkaban.executor.ExecutableFlow;
import azkaban.utils.EmailMessage;

/**
 * 邮件内容创建器
 */
public interface MailCreator {
    /**
     * 创建首次错误邮件消息
     * @param flow
     * @param message
     * @param azkabanName
     * @param scheme
     * @param clientHostname
     * @param clientPortNumber
     * @param vars
     * @return
     */
  public boolean createFirstErrorMessage(ExecutableFlow flow,
      EmailMessage message, String azkabanName, String scheme,
      String clientHostname, String clientPortNumber, String... vars);

    /**
     * 创建任务流错误邮件消息
     * @param flow
     * @param message
     * @param azkabanName
     * @param scheme
     * @param clientHostname
     * @param clientPortNumber
     * @param vars
     * @return
     */
  public boolean createErrorEmail(ExecutableFlow flow, EmailMessage message,
      String azkabanName, String scheme, String clientHostname,
      String clientPortNumber, String... vars);

    /**
     * 创建任务流执行成功的邮件通知消息
     * @param flow
     * @param message
     * @param azkabanName
     * @param scheme
     * @param clientHostname
     * @param clientPortNumber
     * @param vars
     * @return
     */
  public boolean createSuccessEmail(ExecutableFlow flow, EmailMessage message,
      String azkabanName, String scheme, String clientHostname,
      String clientPortNumber, String... vars);
}
