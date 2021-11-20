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

package org.apache.flink.table.connector.source;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.abilities.SupportsAggregatePushDown;
import org.apache.flink.table.connector.source.abilities.SupportsFilterPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsPartitionPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsProjectionPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsReadingMetadata;
import org.apache.flink.table.connector.source.abilities.SupportsSourceWatermark;
import org.apache.flink.table.connector.source.abilities.SupportsWatermarkPushDown;
import org.apache.flink.types.RowKind;

import java.io.Serializable;

/**
 * A {@link DynamicTableSource} that scans all rows from an external storage system during runtime.
 *
 * <p>The scanned rows don't have to contain only insertions but can also contain updates and
 * deletions. Thus, the table source can be used to read a (finite or infinite) changelog. The given
 * {@link ChangelogMode} indicates the set of changes that the planner can expect during runtime.
 *
 * <p>For regular batch scenarios, the source can emit a bounded stream of insert-only rows.
 *
 * <p>For regular streaming scenarios, the source can emit an unbounded stream of insert-only rows.
 *
 * <p>For change data capture (CDC) scenarios, the source can emit bounded or unbounded streams with
 * insert, update, and delete rows. See also {@link RowKind}.
 *
 * <p>A {@link ScanTableSource} can implement the following abilities that might mutate an instance
 * during planning:
 *
 * <ul>
 *   <li>{@link SupportsWatermarkPushDown}
 *   <li>{@link SupportsSourceWatermark}
 *   <li>{@link SupportsFilterPushDown}
 *   <li>{@link SupportsAggregatePushDown}
 *   <li>{@link SupportsProjectionPushDown}
 *   <li>{@link SupportsPartitionPushDown}
 *   <li>{@link SupportsReadingMetadata}
 * </ul>
 *
 * <p>In the last step, the planner will call {@link #getScanRuntimeProvider(ScanContext)} for
 * obtaining a provider of runtime implementation.
 * 一个DynamicTableSource，在运行时扫描来自外部存储系统的所有行。
 * 扫描的行不必只包含插入，还可以包含更新和删除。
 * 因此，可以使用表源来读取(有限或无限)更改日志。
 * 给定的ChangelogMode表示规划器在运行时可以预期的更改集。
 * 对于常规的批处理场景，源可以发出仅插入行的有界流。
 * 对于常规流场景，源可以发出无限制的仅插入行的流。
 * 对于更改数据捕获(CDC)场景，源可以发出带有插入、更新和删除行的有界或无界流。
 * 也看到RowKind。 ScanTableSource可以实现以下能力，可能在规划过程中改变实例: SupportsWatermarkPushDown SupportsSourceWatermark SupportsFilterPushDown SupportsAggregatePushDown SupportsProjectionPushDown SupportsPartitionPushDown SupportsReadingMetadata
 * 在最后一步中，规划器将调用getScanRuntimeProvider(ScanTableSource.ScanContext)来获取运行时实现的提供者。
 */
@PublicEvolving
public interface ScanTableSource extends DynamicTableSource {

    /**
     * Returns the set of changes that the planner can expect during runtime.
     * 返回计划程序在运行时可预期的更改集。
     * @see RowKind
     */
    ChangelogMode getChangelogMode();

    /**
     * Returns a provider of runtime implementation for reading the data.
     *
     * <p>There might exist different interfaces for runtime implementation which is why {@link
     * ScanRuntimeProvider} serves as the base interface. Concrete {@link ScanRuntimeProvider}
     * interfaces might be located in other Flink modules.
     *
     * <p>Independent of the provider interface, the table runtime expects that a source
     * implementation emits internal data structures (see {@link
     * org.apache.flink.table.data.RowData} for more information).
     *
     * <p>The given {@link ScanContext} offers utilities by the planner for creating runtime
     * implementation with minimal dependencies to internal data structures.
     *
     * <p>{@link SourceProvider} is the recommended core interface. {@code SourceFunctionProvider}
     * in {@code flink-table-api-java-bridge} and {@link InputFormatProvider} are available for
     * backwards compatibility.
     *
     * @see SourceProvider
     * 返回用于读取数据的运行时实现的提供程序。 可能存在不同的运行时实现接口，这就是为什么ScanTableSource。
     * ScanRuntimeProvider作为基本接口。
     * 混凝土ScanTableSource。ScanRuntimeProvider接口可能位于其他Flink模块中。
     * 独立于提供程序接口，表运行时期望源实现发出内部数据结构(更多信息请参阅org.apache.flink.table.data.RowData)。
     * 给定的ScanTableSource。
     * ScanContext通过规划器提供实用程序，用于创建运行时实现，并将对内部数据结构的依赖降到最低。
     * SourceProvider是推荐的核心接口。flink-table-api-java-bridge中的SourceFunctionProvider和InputFormatProvider是向后兼容的。
     *
     *
     */
    ScanRuntimeProvider getScanRuntimeProvider(ScanContext runtimeProviderContext);

    // --------------------------------------------------------------------------------------------
    // Helper interfaces
    // --------------------------------------------------------------------------------------------

    /**
     * Context for creating runtime implementation via a {@link ScanRuntimeProvider}.
     *
     * <p>It offers utilities by the planner for creating runtime implementation with minimal
     * dependencies to internal data structures.
     *
     * <p>Methods should be called in {@link #getScanRuntimeProvider(ScanContext)}. The returned
     * instances are {@link Serializable} and can be directly passed into the runtime implementation
     * class.
     */
    interface ScanContext extends DynamicTableSource.Context {
        // may introduce scan specific methods in the future
    }

    /**
     * Provides actual runtime implementation for reading the data.
     *
     * <p>There might exist different interfaces for runtime implementation which is why {@link
     * ScanRuntimeProvider} serves as the base interface. Concrete {@link ScanRuntimeProvider}
     * interfaces might be located in other Flink modules.
     *
     * <p>{@link SourceProvider} is the recommended core interface. {@code SourceFunctionProvider}
     * in {@code flink-table-api-java-bridge} and {@link InputFormatProvider} are available for
     * backwards compatibility.
     */
    interface ScanRuntimeProvider {

        /** Returns whether the data is bounded or not. */
        boolean isBounded();
    }
}
