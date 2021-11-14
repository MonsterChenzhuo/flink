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

package org.apache.flink.api.common.io;

import org.apache.flink.annotation.Public;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.core.io.InputSplitSource;

import java.io.IOException;
import java.io.Serializable;

/**
 * The base interface for data sources that produces records.
 *
 * <p>The input format handles the following:
 *
 * <ul>
 *   <li>It describes how the input is split into splits that can be processed in parallel.
 *   <li>It describes how to read records from the input split.
 *   <li>It describes how to gather basic statistics from the input.
 * </ul>
 *
 * <p>The life cycle of an input format is the following:
 *
 * <ol>
 *
 *   <li>After being instantiated (parameterless), it is configured with a {@link Configuration}
 *       object. Basic fields are read from the configuration, such as a file path, if the format
 *       describes files as input.
 *   <li>Optionally: It is called by the compiler to produce basic statistics about the input.
 *   <li>It is called to create the input splits.
 *   <li>Each parallel input task creates an instance, configures it and opens it for a specific
 *       split.
 *   <li>All records are read from the input
 *   <li>The input format is closed
 * </ol>
 *
 * <p>IMPORTANT NOTE: Input formats must be written such that an instance can be opened again after
 * it was closed. That is due to the fact that the input format is used for potentially multiple
 * splits. After a split is done, the format's close function is invoked and, if another split is
 * available, the open function is invoked afterwards for the next split.
 *
 * @see InputSplit
 * @see BaseStatistics
 * @param <OT> The type of the produced records.
 * @param <T> The type of input split.
 * 产生记录的数据源的基本接口。 输入格式处理以下内容: 它描述了如何将输入分割成可以并行处理的分割。
 * 它描述了如何从输入分割中读取记录。 它描述了如何从输入中收集基本统计信息。
 * 输入格式的生命周期如下: 在被实例化(无参数)之后，它被配置为一个Configuration对象。
 * 如果格式将文件描述为输入，则从配置中读取基本字段，例如文件路径。
 * 可选:编译器调用它来生成关于输入的基本统计信息。 调用它来创建输入分割。
 * 每个并行输入任务创建一个实例，配置它，并为特定的分割打开它。
 * 从输入中读取所有记录 输入格式关闭 重要注意:输入格式的编写必须使实例在关闭后可以再次打开。
 * 这是由于输入格式可能用于多个分割。在一次拆分完成后，将调用格式的close函数，如果有另一个拆分可用，则随后将为下一次拆分调用open函数。
 *
 */
@Public
public interface InputFormat<OT, T extends InputSplit> extends InputSplitSource<T>, Serializable {

    /**
     * Configures this input format. Since input formats are instantiated generically and hence
     * parameterless, this method is the place where the input formats set their basic fields based
     * on configuration values.
     *
     * <p>This method is always called first on a newly instantiated input format.
     *
     * @param parameters The configuration with all parameters (note: not the Flink config but the
     *     TaskConfig).
     */
    void configure(Configuration parameters);

    /**
     * Gets the basic statistics from the input described by this format. If the input format does
     * not know how to create those statistics, it may return null. This method optionally gets a
     * cached version of the statistics. The input format may examine them and decide whether it
     * directly returns them without spending effort to re-gather the statistics.
     *
     * <p>When this method is called, the input format is guaranteed to be configured.
     *
     * @param cachedStatistics The statistics that were cached. May be null.
     * @return The base statistics for the input, or null, if not available.
     */
    BaseStatistics getStatistics(BaseStatistics cachedStatistics) throws IOException;

    // --------------------------------------------------------------------------------------------

    @Override
    T[] createInputSplits(int minNumSplits) throws IOException;

    @Override
    InputSplitAssigner getInputSplitAssigner(T[] inputSplits);

    // --------------------------------------------------------------------------------------------

    /**
     * Opens a parallel instance of the input format to work on a split.
     *
     * <p>When this method is called, the input format it guaranteed to be configured.
     *
     * @param split The split to be opened.
     * @throws IOException Thrown, if the spit could not be opened due to an I/O problem.
     * 打开输入格式的并行实例以处理拆分。
     * 当调用此方法时，它保证已配置的输入格式。
     * 参数: split -要打开的裂口。
     * 抛出: IOException -如果由于I/O问题而无法打开唾液，则抛出。
     */
    void open(T split) throws IOException;

    /**
     * Method used to check if the end of the input is reached.
     *
     * <p>When this method is called, the input format it guaranteed to be opened.
     *
     * @return True if the end is reached, otherwise false.
     * @throws IOException Thrown, if an I/O error occurred.
     */
    boolean reachedEnd() throws IOException;

    /**
     * Reads the next record from the input.
     *
     * <p>When this method is called, the input format it guaranteed to be opened.
     *
     * @param reuse Object that may be reused.
     * @return Read record.
     * @throws IOException Thrown, if an I/O error occurred.
     * 从输入中读取下一条记录。 当调用此方法时，它保证打开的输入格式。
     * 参数: reuse—可重用的对象。
     * 返回值: 阅读记录。
     * 抛出: IOException -当I/O发生错误时抛出。
     */
    OT nextRecord(OT reuse) throws IOException;

    /**
     * Method that marks the end of the life-cycle of an input split. Should be used to close
     * channels and streams and release resources. After this method returns without an error, the
     * input is assumed to be correctly read.
     *
     * <p>When this method is called, the input format it guaranteed to be opened.
     *
     * @throws IOException Thrown, if the input could not be closed properly.
     */
    void close() throws IOException;
}
