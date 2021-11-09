/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.functions.source;

import org.apache.flink.annotation.PublicEvolving;

/**
 * The mode in which the {@link ContinuousFileMonitoringFunction} operates. This can be either
 * {@link #PROCESS_ONCE} or {@link #PROCESS_CONTINUOUSLY}.
 * ContinuousFileMonitoringFunction运行的模式。
 * 这可以是PROCESS_ONCE或PROCESS_CONTINUOUSLY。
 */
@PublicEvolving
public enum FileProcessingMode {

    /** Processes the current contents of the path and exits. */
    /** 处理路径的当前内容并退出。*/
    PROCESS_ONCE,

    /** Periodically scans the path for new data. */
    /** 定期扫描路径是否有新数据。*/
    PROCESS_CONTINUOUSLY
}
