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

package azkaban.execapp.event;

import azkaban.executor.Status;

/**
 * 封装对任务特定状态的阻塞等待，任务状态的修改
 * 对应于一次任务的执行中状态变迁，状态的等待
 */
public class BlockingStatus {
  private static final long WAIT_TIME = 5 * 60 * 1000;
  private final int execId;
  private final String jobId;
  private Status status;

  public BlockingStatus(int execId, String jobId, Status initialStatus) {
    this.execId = execId;
    this.jobId = jobId;
    this.status = initialStatus;
  }

  public Status blockOnFinishedStatus() {
    if (status == null) {
      return null;
    }

    while (!Status.isStatusFinished(status)) {
      synchronized (this) {
        try {
          this.wait(WAIT_TIME);
        } catch (InterruptedException e) {
        }
      }
    }

    return status;
  }

  public Status viewStatus() {
    return this.status;
  }

  public void unblock() {
    synchronized (this) {
      this.notifyAll();
    }
  }

  public void changeStatus(Status status) {
    synchronized (this) {
      this.status = status;
      if (Status.isStatusFinished(status)) {
        unblock();
      }
    }
  }

  public int getExecId() {
    return execId;
  }

  public String getJobId() {
    return jobId;
  }
}
