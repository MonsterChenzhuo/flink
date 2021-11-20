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
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.RuntimeConverter;
import org.apache.flink.table.connector.source.abilities.SupportsFilterPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsProjectionPushDown;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.types.Row;

import javax.annotation.Nullable;

import java.io.Serializable;

/**
 * Source of a dynamic table from an external storage system.
 *
 * <p>Dynamic tables are the core concept of Flink's Table & SQL API for processing both bounded and
 * unbounded data in a unified fashion. By definition, a dynamic table can change over time.
 *
 * <p>When reading a dynamic table, the content can either be considered as:
 *
 * <ul>
 *   <li>A changelog (finite or infinite) for which all changes are consumed continuously until the
 *       changelog is exhausted. See {@link ScanTableSource} for more information.
 *   <li>A continuously changing or very large external table whose content is usually never read
 *       entirely but queried for individual values when necessary. See {@link LookupTableSource}
 *       for more information.
 * </ul>
 *
 * <p>Note: Both interfaces can be implemented at the same time. The planner decides about their
 * usage depending on the specified query.
 *
 * <p>Instances of the above-mentioned interfaces can be seen as factories that eventually produce
 * concrete runtime implementation for reading the actual data.
 *
 * <p>Depending on the optionally declared abilities such as {@link SupportsProjectionPushDown} or
 * {@link SupportsFilterPushDown}, the planner might apply changes to an instance and thus mutates
 * the produced runtime implementation.
 * 来自外部存储系统的动态表的源。 动态表是Flink的Table & SQL API的核心概念，用于以统一的方式处理有界和无界数据。
 * 根据定义，动态表可以随时间变化。
 * 当读取一个动态表时，内容可以被认为是: 一种变更记录(有限的或无限的)，它的所有变更都被连续地消耗，直到耗尽该变更记录。
 * 更多信息请参见ScanTableSource。
 * 一个不断变化的或非常大的外部表，它的内容通常不会被完全读取，而是在必要时查询单个值。
 * 有关更多信息，请参阅LookupTableSource。
 * 注意:两个接口可以同时实现。计划器根据指定的查询决定它们的使用。
 * 可以将上述接口的实例视为工厂，它们最终生成具体的运行时实现来读取实际数据。
 * 根据可选声明的功能(如SupportsProjectionPushDown或SupportsFilterPushDown)，规划器可能会将更改应用到实例，从而改变生成的运行时实现。
 *
 */
@PublicEvolving
public interface DynamicTableSource {

    /**
     * Creates a copy of this instance during planning. The copy should be a deep copy of all
     * mutable members.
     * 在规划期间创建此实例的副本。副本应该是所有可变成员的深层副本。
     */
    DynamicTableSource copy();

    /** Returns a string that summarizes this source for printing to a console or log. */
    /** 返回一个字符串，该字符串汇总此源以便打印到控制台或日志。*/
    String asSummaryString();

    // --------------------------------------------------------------------------------------------
    // Helper interfaces
    // --------------------------------------------------------------------------------------------

    /**
     * Base context for creating runtime implementation via a {@link
     * ScanTableSource.ScanRuntimeProvider} and {@link LookupTableSource.LookupRuntimeProvider}.
     *
     * <p>It offers utilities by the planner for creating runtime implementation with minimal
     * dependencies to internal data structures.
     *
     * <p>Methods should be called in {@link
     * ScanTableSource#getScanRuntimeProvider(ScanTableSource.ScanContext)} and {@link
     * LookupTableSource#getLookupRuntimeProvider(LookupTableSource.LookupContext)}. The returned
     * instances are {@link Serializable} and can be directly passed into the runtime implementation
     * class.
     */
    interface Context {

        /**
         * Creates type information describing the internal data structures of the given {@link
         * DataType}.
         *
         * @see ResolvedSchema#toPhysicalRowDataType()
         */
        <T> TypeInformation<T> createTypeInformation(DataType producedDataType);

        /**
         * Creates type information describing the internal data structures of the given {@link
         * LogicalType}.
         */
        <T> TypeInformation<T> createTypeInformation(LogicalType producedLogicalType);

        /**
         * Creates a converter for mapping between objects specified by the given {@link DataType}
         * and Flink's internal data structures that can be passed into a runtime implementation.
         *
         * <p>For example, a {@link Row} and its fields can be converted into {@link RowData}, or a
         * (possibly nested) POJO can be converted into the internal representation for structured
         * types.
         *
         * @see LogicalType#supportsInputConversion(Class)
         * @see ResolvedSchema#toPhysicalRowDataType()
         */
        DataStructureConverter createDataStructureConverter(DataType producedDataType);
    }

    /**
     * Converter for mapping between objects and Flink's internal data structures during runtime.
     *
     * <p>On request, the planner will provide a specialized (possibly code generated) converter
     * that can be passed into a runtime implementation.
     *
     * <p>For example, a {@link Row} and its fields can be converted into {@link RowData}, or a
     * (possibly nested) POJO can be converted into the internal representation for structured
     * types.
     *
     * @see LogicalType#supportsInputConversion(Class)
     */
    interface DataStructureConverter extends RuntimeConverter {

        /** Converts the given object into an internal data structure. */
        @Nullable
        Object toInternal(@Nullable Object externalStructure);
    }
}
