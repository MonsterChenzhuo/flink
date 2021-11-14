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

package org.apache.flink.table.factories;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.configuration.ConfigOption;

import java.util.Set;

/**
 * Base interface for all kind of factories that create object instances from a list of key-value
 * pairs in Flink's Table & SQL API.
 *
 * <p>A factory is uniquely identified by {@link Class} and {@link #factoryIdentifier()}.
 *
 * <p>The list of available factories is discovered using Java's Service Provider Interfaces (SPI).
 * Classes that implement this interface can be added to {@code
 * META_INF/services/org.apache.flink.table.factories.Factory} in JAR files.
 *
 * <p>Every factory declares a set of required and optional options. This information will not be
 * used during discovery but is helpful when generating documentation and performing validation. A
 * factory may discover further (nested) factories, the options of the nested factories must not be
 * declared in the sets of this factory.
 *
 * <p>It is the responsibility of each factory to perform validation before returning an instance.
 *
 * <p>For consistency, the following style for key names of {@link ConfigOption} is recommended:
 *
 * <ul>
 *   <li>Try to <b>reuse</b> key names as much as possible. Use other factory implementations as an
 *       example.
 *   <li>Key names should be declared in <b>lower case</b>. Use "-" instead of dots or camel case to
 *       split words.
 *   <li>Key names should be <b>hierarchical</b> where appropriate. Think about how one would define
 *       such a hierarchy in JSON or YAML file (e.g. {@code sink.bulk-flush.max-actions}).
 *   <li>In case of a hierarchy, try not to use the higher level again in the key name (e.g. do
 *       {@code sink.partitioner} instead of {@code sink.sink-partitioner}) to <b>keep the keys
 *       short</b>.
 *   <li>Key names which can be templated, e.g. to refer to a specific column, should be listed
 *       using '#' as the placeholder symbol. For example, use {@code fields.#.min}.
 * </ul>
 * 用于所有类型的工厂的基本接口，这些工厂从Flink的Table & SQL API中的键值对列表中创建对象实例。
 * 工厂由Class和factoryIdentifier()唯一标识。 可用工厂列表是使用Java的服务提供程序接口(SPI)发现的。
 * 实现此接口的类可以添加到META_INF/services/org.apache.flink.table.factories中。JAR文件中的工厂。
 * 每个工厂声明一组必需和可选选项。此信息在发现期间不会使用，但在生成文档和执行验证时很有帮助。
 * 一个工厂可能会发现更多的(嵌套的)工厂，嵌套工厂的选项不能在这个工厂的集合中声明。
 * 在返回实例之前执行验证是每个工厂的责任。 为了一致性，建议ConfigOption的键名采用以下样式: 尽量重用键名。
 * 以其他工厂实现为例。 键名应该用小写声明。使用“-”而不是点或驼峰格来分割单词。 键名应该在适当的地方分层。
 * 想想如何在JSON或YAML文件中定义这样的层次结构(例如，sink.bulk-flush.max-actions)。
 * 如果是层次结构，尽量不要在键名中再次使用较高层(例如do sink)。Partitioner而不是sink.sink-partitioner)以保持键短。
 * 可以被模板化的键名，例如指向特定的列，应该使用'#'作为占位符来列出。例如，使用字段。#.min。
 */
@PublicEvolving
public interface Factory {

    /**
     * Returns a unique identifier among same factory interfaces.
     *
     * <p>For consistency, an identifier should be declared as one lower case word (e.g. {@code
     * kafka}). If multiple factories exist for different versions, a version should be appended
     * using "-" (e.g. {@code elasticsearch-7}).
     * 返回相同工厂接口之间的唯一标识符。 为了一致性，标识符应该声明为一个小写单词(例如kafka)。
     * 如果存在多个不同版本的工厂，则应该使用"-"附加一个版本(例如elasticsearch-7)。
     */
    String factoryIdentifier();

    /**
     * Returns a set of {@link ConfigOption} that an implementation of this factory requires in
     * addition to {@link #optionalOptions()}.
     *
     * <p>See the documentation of {@link Factory} for more information.
     * 返回一组ConfigOption，除了optionalOptions()，该工厂的实现还需要ConfigOption。
     */
    Set<ConfigOption<?>> requiredOptions();

    /**
     * Returns a set of {@link ConfigOption} that an implementation of this factory consumes in
     * addition to {@link #requiredOptions()}.
     *
     * <p>See the documentation of {@link Factory} for more information.
     * 返回一组ConfigOption，除了requiredOptions()，该工厂的实现还使用它。 有关更多信息，请参阅Factory的文档
     */
    Set<ConfigOption<?>> optionalOptions();
}
