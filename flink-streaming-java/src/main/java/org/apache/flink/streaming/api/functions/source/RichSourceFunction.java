/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.functions.source;

import org.apache.flink.annotation.Public;
import org.apache.flink.api.common.functions.AbstractRichFunction;

/**
 * Base class for implementing a parallel data source that has access to context information (via
 * {@link #getRuntimeContext()}) and additional life-cycle methods ({@link
 * #open(org.apache.flink.configuration.Configuration)} and {@link #close()}.
 *
 * <p>This class is useful when implementing parallel sources where different parallel subtasks need
 * to perform different work. Typical patterns for that are:
 *
 * <ul>
 *   <li>Use {@link #getRuntimeContext()} to obtain the runtime context.
 *   <li>Use {@link
 *       org.apache.flink.api.common.functions.RuntimeContext#getNumberOfParallelSubtasks()} to
 *       determine the current parallelism. It is strongly encouraged to use this method, rather
 *       than hard-wiring the parallelism, because the configured parallelism may change depending
 *       on program configuration. The parallelism may also change after recovering failures, when
 *       fewer than desired parallel worker as available.
 *   <li>Use {@link org.apache.flink.api.common.functions.RuntimeContext#getIndexOfThisSubtask()} to
 *       determine which subtask the current instance of the function executes.
 * </ul>
 *
 * @param <OUT> The type of the records produced by this source.
 * 实现并行数据源的基类，该数据源可以访问上下文信息(通过{@link getRuntimeContext()})
 * 和其他生命周期方法({@link open(org.apache.flink.configuration.Configuration)}和{@link close()}。
 * <p>这个类在实现并行源时很有用，因为不同的并行子任务需要执行不同的工作。
 * 典型的模式是:<ul> <li>使用{@link getRuntimeContext()}来获取运行时上下文。
 * <li>使用{@link org.apache.flink.api.common.functions.RuntimeContextgetNumberOfParallelSubtasks()}来确定当前的并行度。
 * 强烈建议使用此方法，而不是硬连接并行性，因为配置的并行性可能会根据程序配置而改变。
 * 在恢复失败后，当可用的并行工作人员少于所需时，并行性也可能发生变化。
 * <li>使用{@link org.apache.flink.api.common.functions.RuntimeContextgetIndexOfThisSubtask()}来确定函数的当前实例执行哪个子任务。
 * <ul> @param <OUT>此源产生的记录的类型。
 *
 */
@Public
public abstract class RichSourceFunction<OUT> extends AbstractRichFunction
        implements SourceFunction<OUT> {

    private static final long serialVersionUID = 1L;
}
