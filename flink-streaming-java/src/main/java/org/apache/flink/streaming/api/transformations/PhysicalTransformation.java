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

package org.apache.flink.streaming.api.transformations;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.dag.Transformation;
import org.apache.flink.streaming.api.operators.ChainingStrategy;

/**
 * A {@link Transformation} that creates a physical operation. It enables setting {@link
 * ChainingStrategy}.
 *
 * @param <T> The type of the elements that result from this {@code Transformation}
 * @see Transformation
 * 创建物理操作的转换。它能够设置ChainingStrategy。请参阅: 转换
 * 类型参数:  -由此转换产生的元素的类型
 */
@Internal
public abstract class PhysicalTransformation<T> extends Transformation<T> {

    /**
     * Creates a new {@code Transformation} with the given name, output type and parallelism.
     *
     * @param name The name of the {@code Transformation}, this will be shown in Visualizations and
     *     the Log
     * @param outputType The output type of this {@code Transformation}
     * @param parallelism The parallelism of this {@code Transformation}
     * 使用给定的名称、输出类型和并行性创建一个新的Transformation。
     * 参数: name -转换的名称，这将在可视化和日志中显示
     *      outputType -此转换的输出类型 这个变换的并行性
     *
     */
    PhysicalTransformation(String name, TypeInformation<T> outputType, int parallelism) {
        super(name, outputType, parallelism);
    }

    /** Sets the chaining strategy of this {@code Transformation}. */
    public abstract void setChainingStrategy(ChainingStrategy strategy);
}
