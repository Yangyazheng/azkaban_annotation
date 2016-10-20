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

package azkaban.webapp.servlet;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * 便捷实用的工具类，
 * 目前只是时间格式化
 */
public class VelocityUtils {
    /**
     * 格式化时间
     * @param timestamp
     * @return
     */
    public String formatDate(long timestamp) {
        return formatDate(timestamp, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 格式化时间
     * @param date
     * @return
     */
    public String formatDate(DateTime date) {
        return formatDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 格式化时间
     * @param timestamp
     * @param format
     * @return
     */
    public String formatDate(long timestamp, String format) {
        DateTimeFormatter f = DateTimeFormat.forPattern(format);
        return f.print(timestamp);
    }

    /**
     * 格式化时间
     * @param date
     * @param format
     * @return
     */
    public String formatDate(DateTime date, String format) {
        DateTimeFormatter f = DateTimeFormat.forPattern(format);
        return f.print(date);
    }
}
