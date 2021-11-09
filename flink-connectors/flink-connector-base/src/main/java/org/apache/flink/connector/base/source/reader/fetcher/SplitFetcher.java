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

package org.apache.flink.connector.base.source.reader.fetcher;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.connector.source.SourceSplit;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.splitreader.SplitReader;
import org.apache.flink.connector.base.source.reader.synchronization.FutureCompletingBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 内部获取器可运行，负责从外部系统轮询消息。 */
/** The internal fetcher runnable responsible for polling message from the external system. */
@Internal
public class SplitFetcher<E, SplitT extends SourceSplit> implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SplitFetcher.class);
    private static final SplitFetcherTask WAKEUP_TASK = new DummySplitFetcherTask("WAKEUP_TASK");

    private final int id;
    private final BlockingDeque<SplitFetcherTask> taskQueue;
    // track the assigned splits so we can suspend the reader when there is no splits assigned.
    //跟踪分配的分割，这样当没有分配的分割时，我们可以挂起阅读器
    private final Map<String, SplitT> assignedSplits;
    private final FutureCompletingBlockingQueue<RecordsWithSplitIds<E>> elementsQueue;
    private final SplitReader<E, SplitT> splitReader;
    private final Consumer<Throwable> errorHandler;
    private final Runnable shutdownHook;
    private final AtomicBoolean wakeUp;
    private final AtomicBoolean closed;
    private final FetchTask<E, SplitT> fetchTask;
    private volatile SplitFetcherTask runningTask = null;

    private final Object lock = new Object();

    /**
     * Flag whether this fetcher has no work assigned at the moment. Fetcher that have work (a
     * split) assigned but are currently blocked (for example enqueueing a fetch and hitting the
     * element queue limit) are NOT considered idle.
     * 标志这个取件器目前是否没有分配工作。
     * 已经分配了工作(一个分割)但当前阻塞的取操作(例如进入取操作队列并达到元素队列限制)不被认为是空闲的。
     */
    @GuardedBy("lock")
    private volatile boolean isIdle;

    SplitFetcher(
            int id,
            FutureCompletingBlockingQueue<RecordsWithSplitIds<E>> elementsQueue,
            SplitReader<E, SplitT> splitReader,
            Consumer<Throwable> errorHandler,
            Runnable shutdownHook,
            Consumer<Collection<String>> splitFinishedHook) {
        this.id = id;
        this.taskQueue = new LinkedBlockingDeque<>();
        this.elementsQueue = elementsQueue;
        this.assignedSplits = new HashMap<>();
        this.splitReader = splitReader;
        this.errorHandler = errorHandler;
        this.shutdownHook = shutdownHook;
        this.isIdle = true;
        this.wakeUp = new AtomicBoolean(false);
        this.closed = new AtomicBoolean(false);

        this.fetchTask =
                new FetchTask<>(
                        splitReader,
                        elementsQueue,
                        ids -> {
                            ids.forEach(assignedSplits::remove);
                            splitFinishedHook.accept(ids);
                            LOG.info("Finished reading from splits {}", ids);
                        },
                        id);
    }

    @Override
    public void run() {
        LOG.info("Starting split fetcher {}", id);
        try {
            while (!closed.get()) {
                runOnce();
            }
        } catch (Throwable t) {
            errorHandler.accept(t);
        } finally {
            try {
                splitReader.close();
            } catch (Exception e) {
                errorHandler.accept(e);
            }
            LOG.info("Split fetcher {} exited.", id);
            // This executes after possible errorHandler.accept(t). If these operations bear
            // a happens-before relation, then we can checking side effect of errorHandler.accept(t)
            // to know whether it happened after observing side effect of shutdownHook.run().
            //它在可能的errorHandler.accept(t)之后执行。如果这些操作具有happens-before关系，
            // 则可以在观察shutdownHook.run()的副作用后，检查errorHandler.accept(t)的副作用，以了解它是否发生。
            shutdownHook.run();
        }
    }

    /** 打包私有方法来帮助单元测试 */
    /** Package private method to help unit test. */
    void runOnce() {
        try {
            // The fetch task should run if the split assignment is not empty or there is a split
            // change.
            if (shouldRunFetchTask()) {
                runningTask = fetchTask;
            } else {
                runningTask = taskQueue.take();
            }
            // Now the running task is not null. If wakeUp() is called after this point,
            // task.wakeUp() will be called. On the other hand, if the wakeUp() call was make before
            // this point, the wakeUp flag must have already been set. The code hence checks the
            // wakeUp
            // flag first to avoid an unnecessary task run.
            // Note that the runningTask may still encounter the case that the task is waken up
            // before
            // the it starts running.
            LOG.debug("Prepare to run {}", runningTask);
            if (!wakeUp.get() && runningTask.run()) {
                LOG.debug("Finished running task {}", runningTask);
                // the task has finished running. Set it to null so it won't be enqueued.
                runningTask = null;
                checkAndSetIdle();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "SplitFetcher thread %d received unexpected exception while polling the records",
                            id),
                    e);
        }
        // If the task is not null that means this task needs to be re-executed. This only
        // happens when the task is the fetching task or the task was interrupted.
        maybeEnqueueTask(runningTask);
        synchronized (wakeUp) {
            // Set the running task to null. It is necessary for the shutdown method to avoid
            // unnecessarily interrupt the running task.
            runningTask = null;
            // Set the wakeUp flag to false.
            wakeUp.set(false);
            LOG.debug("Cleaned wakeup flag.");
        }
    }

    /**
     * Add splits to the split fetcher. This operation is asynchronous.
     *
     * @param splitsToAdd the splits to add.
     * 向分割取取器添加分割。此操作为异步操作。
     * 参数: splitsToAdd—要添加的分割。
     */
    public void addSplits(List<SplitT> splitsToAdd) {
        enqueueTask(new AddSplitsTask<>(splitReader, splitsToAdd, assignedSplits));
        wakeUp(true);
    }

    public void enqueueTask(SplitFetcherTask task) {
        synchronized (lock) {
            taskQueue.offer(task);
            isIdle = false;
        }
    }

    public SplitReader<E, SplitT> getSplitReader() {
        return splitReader;
    }

    public int fetcherId() {
        return id;
    }

    /** Shutdown the split fetcher. */
    public void shutdown() {
        if (closed.compareAndSet(false, true)) {
            LOG.info("Shutting down split fetcher {}", id);
            wakeUp(false);
        }
    }

    /**
     * Package private for unit test.
     *
     * @return the assigned splits.
     * 包私有的单元测试。
     * 返回值: 分配的分裂
     */
    Map<String, SplitT> assignedSplits() {
        return assignedSplits;
    }

    /**
     * Package private for unit test.
     *
     * @return true if task queue is not empty, false otherwise.
     */
    boolean isIdle() {
        return isIdle;
    }

    /**
     * Check whether the fetch task should run. The fetch task should only run when all the
     * following conditions are met. 1. there is no task in the task queue to run. 2. there are
     * assigned splits Package private for testing purpose.
     *
     * @return whether the fetch task should be run.
     */
    boolean shouldRunFetchTask() {
        return taskQueue.isEmpty() && !assignedSplits.isEmpty();
    }

    /**
     * Wake up the fetcher thread. There are only two blocking points in a running fetcher. 1.
     * Taking the next task out of the task queue. 2. Running a task.
     *
     * <p>They need to be waken up differently. If the fetcher is blocking waiting on the next task
     * in the task queue, we should just interrupt the fetcher thread. If the fetcher is running the
     * user split reader, we should call SplitReader.wakeUp() instead of naively interrupt the
     * thread.
     *
     * <p>The correctness can be think of in the following way. The purpose of wake up is to let the
     * fetcher thread go to the very beginning of the running loop. There are three major events in
     * each run of the loop.
     *
     * <ol>
     *   <li>pick a task (blocking)
     *   <li>assign the task to runningTask variable.
     *   <li>run the runningTask. (blocking)
     * </ol>
     *
     * <p>We don't need to worry about things after step 3 because there is no blocking point
     * anymore.
     *
     * <p>We always first set the wakeup flag when waking up the fetcher, then use the value of
     * running task to determine where the fetcher thread is.
     *
     * <ul>
     *   <li>If runningThread is null, it is before step 2, so we should interrupt fetcher. This
     *       interruption will not be propagated to the split reader, because the wakeUp flag will
     *       prevent the fetchTask from running.
     *   <li>If runningThread is not null, it is after step 2. so we should wakeUp the split reader
     *       instead of interrupt the fetcher.
     * </ul>
     *
     * <p>The above logic only works in the same {@link #runOnce()} invocation. So we need to
     * synchronize to ensure the wake up logic do not touch a different invocation.
     * 唤醒获取线程。在一个正在运行的获取器中只有两个阻塞点。1. 从任务队列中取出下一个任务。2. 运行一个任务。
     * 他们需要以不同的方式被唤醒。如果获取器阻塞等待任务队列中的下一个任务，我们应该直接中断获取线程。
     * 如果读取器正在运行用户split reader，我们应该调用SplitReader.wakeUp()而不是天真地中断线程。
     * 正确性可以用下面的方法来考虑。唤醒的目的是让获取线程回到运行循环的最开始。在循环的每次运行中有三个主要事件。
     * 选择任务(阻塞) 将任务分配给runningTask变量。 运行runningTask。(阻碍) 我们不需要担心步骤3之后的事情，因为不再有阻塞点了。
     * 我们总是在唤醒取线程时首先设置唤醒标志，然后使用运行任务的值来确定取线程的位置。
     * 如果runningThread是null，那么它在第2步之前，所以我们应该中断取线程。
     * 这个中断不会传播到分割读取器，因为wakeUp标志将阻止fetchTask运行。
     * 如果runningThread不为空，则它在步骤2之后。所以我们应该唤醒分割读取器而不是中断读取器。
     * 上述逻辑仅在相同的runOnce()调用中有效。因此，我们需要进行同步，以确保唤醒逻辑不会触及不同的调用。
     */
    void wakeUp(boolean taskOnly) {
        // Synchronize to make sure the wake up only works for the current invocation of runOnce().
        synchronized (wakeUp) {
            // Do not wake up repeatedly.
            wakeUp.set(true);
            // Now the wakeUp flag is set.
            SplitFetcherTask currentTask = runningTask;
            if (isRunningTask(currentTask)) {
                // The running task may have missed our wakeUp flag and running, wake it up.
                LOG.debug("Waking up running task {}", currentTask);
                currentTask.wakeUp();
            } else if (!taskOnly) {
                // The task has not started running yet, and it will not run for this
                // runOnce() invocation due to the wakeUp flag. But we might have to
                // wake up the fetcher thread in case it is blocking on the task queue.
                // Only wake up when the thread has started and there is no running task.
                LOG.debug("Waking up fetcher thread.");
                taskQueue.add(WAKEUP_TASK);
            }
        }
    }

    private void maybeEnqueueTask(SplitFetcherTask task) {
        // Only enqueue unfinished non-fetch task.
        if (!closed.get()
                && isRunningTask(task)
                && task != fetchTask
                && !taskQueue.offerFirst(task)) {
            throw new RuntimeException(
                    "The task queue is full. This is only theoretically possible when really bad thing happens.");
        }
        if (task != null) {
            LOG.debug("Enqueued task {}", task);
        }
    }

    private boolean isRunningTask(SplitFetcherTask task) {
        return task != null && task != WAKEUP_TASK;
    }

    private void checkAndSetIdle() {
        if (shouldIdle()) {
            synchronized (lock) {
                if (shouldIdle()) {
                    isIdle = true;
                }
            }

            // because the method might get invoked past the point when the source reader last
            // checked
            // the elements queue, we need to notify availability in the case when we become idle
            elementsQueue.notifyAvailable();
        }
    }

    private boolean shouldIdle() {
        return assignedSplits.isEmpty() && taskQueue.isEmpty();
    }

    // --------------------- Helper class ------------------

    private static class DummySplitFetcherTask implements SplitFetcherTask {
        private final String name;

        private DummySplitFetcherTask(String name) {
            this.name = name;
        }

        @Override
        public boolean run() {
            return false;
        }

        @Override
        public void wakeUp() {}

        @Override
        public String toString() {
            return name;
        }
    }
}
