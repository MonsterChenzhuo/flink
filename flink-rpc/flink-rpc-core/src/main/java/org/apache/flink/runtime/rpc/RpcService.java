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

package org.apache.flink.runtime.rpc;

import org.apache.flink.runtime.rpc.exceptions.RpcConnectionException;
import org.apache.flink.util.concurrent.ScheduledExecutor;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Interface for rpc services. An rpc service is used to start and connect to a {@link RpcEndpoint}.
 * Connecting to a rpc server will return a {@link RpcGateway} which can be used to call remote
 * procedures.
 * rpc服务的接口。
 * rpc服务用于启动和连接到rpcentpoint。连接到rpc服务器将返回一个可用于调用远程过程的RpcGateway。
 *对应Akka组件是ActorSystem
 */
public interface RpcService {

    /**
     * Return the hostname or host address under which the rpc service can be reached. If the rpc
     * service cannot be contacted remotely, then it will return an empty string.
     *
     * @return Address of the rpc service or empty string if local rpc service
     * 返回可以到达rpc服务的主机名或主机地址。
     * 如果无法远程联系rpc服务，那么它将返回一个空字符串。
     * 返回值: rpc服务地址，如果是本地rpc服务，则为空字符串
     */
    String getAddress();

    /**
     * Return the port under which the rpc service is reachable. If the rpc service cannot be
     * contacted remotely, then it will return -1.
     *
     * @return Port of the rpc service or -1 if local rpc service
     * 返回rpc服务可达的端口。如果无法远程联系rpc服务，那么它将返回-1。
     * 返回值: rpc服务的端口，如果是本地rpc服务，则为-1
     */
    int getPort();

    /**
     * Connect to a remote rpc server under the provided address. Returns a rpc gateway which can be
     * used to communicate with the rpc server. If the connection failed, then the returned future
     * is failed with a {@link RpcConnectionException}.
     *
     * @param address Address of the remote rpc server
     * @param clazz Class of the rpc gateway to return
     * @param <C> Type of the rpc gateway to return
     * @return Future containing the rpc gateway or an {@link RpcConnectionException} if the
     *     connection attempt failed
     * 在提供的地址下连接到远程rpc服务器。
     * 返回一个可用于与rpc服务器通信的rpc网关。
     * 如果连接失败，则返回的future将以RpcConnectionException失败。
     * 参数: address—远程rpc服务器的地址 要返回的rpc网关的类
     * 类型参数:  - rpc网关返回的类型
     * 返回值: 如果连接尝试失败，则包含rpc网关或RpcConnectionException
     *     TODO 远程调用的方法
     */
    <C extends RpcGateway> CompletableFuture<C> connect(String address, Class<C> clazz);

    /**
     * Connect to a remote fenced rpc server under the provided address. Returns a fenced rpc
     * gateway which can be used to communicate with the rpc server. If the connection failed, then
     * the returned future is failed with a {@link RpcConnectionException}.
     *
     * @param address Address of the remote rpc server
     * @param fencingToken Fencing token to be used when communicating with the server
     * @param clazz Class of the rpc gateway to return
     * @param <F> Type of the fencing token
     * @param <C> Type of the rpc gateway to return
     * @return Future containing the fenced rpc gateway or an {@link RpcConnectionException} if the
     *     connection attempt failed
     * 在提供的地址下连接到远程防护rpc服务器。
     * 返回一个防护rpc网关，该网关可用于与rpc服务器通信。如果连接失败，则返回的future将以RpcConnectionException失败。
     * 参数: address—远程rpc服务器的地址 fencingToken—与服务器通信时使用的Fencing令牌 要返回的rpc网关的类
     * 类型参数:  -击剑令牌的类型  - rpc网关返回的类型
     * 返回值: Future包含fenced rpc网关或RpcConnectionException(如果连接尝试失败)
     */
    <F extends Serializable, C extends FencedRpcGateway<F>> CompletableFuture<C> connect(
            String address, F fencingToken, Class<C> clazz);

    /**
     * Start a rpc server which forwards the remote procedure calls to the provided rpc endpoint.
     *
     * @param rpcEndpoint Rpc protocol to dispatch the rpcs to
     * @param <C> Type of the rpc endpoint
     * @return Self gateway to dispatch remote procedure calls to oneself
     * 启动rpc服务器，将远程过程调用转发到提供的rpc端点。
     * 参数: 用于分派Rpc到的Rpc协议
     * 类型参数:  - rpc端点的类型
     * 返回值: 自网关，用于向自己分派远程过程调用
     */
    <C extends RpcEndpoint & RpcGateway> RpcServer startServer(C rpcEndpoint);

    /**
     * Fence the given RpcServer with the given fencing token.
     *
     * <p>Fencing the RpcServer means that we fix the fencing token to the provided value. All RPCs
     * will then be enriched with this fencing token. This expects that the receiving RPC endpoint
     * extends {@link FencedRpcEndpoint}.
     *
     * @param rpcServer to fence with the given fencing token
     * @param fencingToken to fence the RpcServer with
     * @param <F> type of the fencing token
     * @return Fenced RpcServer
     */
    <F extends Serializable> RpcServer fenceRpcServer(RpcServer rpcServer, F fencingToken);

    /**
     * Stop the underlying rpc server of the provided self gateway.
     *
     * @param selfGateway Self gateway describing the underlying rpc server
     * 停止所提供的自我网关的底层rpc服务器。
     * 参数: selfGateway -描述底层rpc服务器的自网关
     */
    void stopServer(RpcServer selfGateway);

    /**
     * Trigger the asynchronous stopping of the {@link RpcService}.
     *
     * @return Future which is completed once the {@link RpcService} has been fully stopped.
     * 触发RpcService的异步停止。
     * 返回值: 当RpcService完全停止时，它将完成。
     */
    CompletableFuture<Void> stopService();

    /**
     * Returns a future indicating when the RPC service has been shut down.
     *
     * @return Termination future
     * 返回一个future，指示RPC服务何时被关闭。
     * 返回值: 终止未来
     */
    CompletableFuture<Void> getTerminationFuture();

    /**
     * Gets a scheduled executor from the RPC service. This executor can be used to schedule tasks
     * to be executed in the future.
     *
     * <p><b>IMPORTANT:</b> This executor does not isolate the method invocations against any
     * concurrent invocations and is therefore not suitable to run completion methods of futures
     * that modify state of an {@link RpcEndpoint}. For such operations, one needs to use the {@link
     * RpcEndpoint#getMainThreadExecutor() MainThreadExecutionContext} of that {@code RpcEndpoint}.
     *
     * @return The RPC service provided scheduled executor
     */
    ScheduledExecutor getScheduledExecutor();

    /**
     * Execute the runnable in the execution context of this RPC Service, as returned by {@link
     * #getScheduledExecutor()} ()}, after a scheduled delay.
     *
     * @param runnable Runnable to be executed
     * @param delay The delay after which the runnable will be executed
     */
    ScheduledFuture<?> scheduleRunnable(Runnable runnable, long delay, TimeUnit unit);

    /**
     * Execute the given runnable in the executor of the RPC service. This method can be used to run
     * code outside of the main thread of a {@link RpcEndpoint}.
     *
     * <p><b>IMPORTANT:</b> This executor does not isolate the method invocations against any
     * concurrent invocations and is therefore not suitable to run completion methods of futures
     * that modify state of an {@link RpcEndpoint}. For such operations, one needs to use the {@link
     * RpcEndpoint#getMainThreadExecutor() MainThreadExecutionContext} of that {@code RpcEndpoint}.
     *
     * @param runnable to execute
     */
    void execute(Runnable runnable);

    /**
     * Execute the given callable and return its result as a {@link CompletableFuture}. This method
     * can be used to run code outside of the main thread of a {@link RpcEndpoint}.
     *
     * <p><b>IMPORTANT:</b> This executor does not isolate the method invocations against any
     * concurrent invocations and is therefore not suitable to run completion methods of futures
     * that modify state of an {@link RpcEndpoint}. For such operations, one needs to use the {@link
     * RpcEndpoint#getMainThreadExecutor() MainThreadExecutionContext} of that {@code RpcEndpoint}.
     *
     * @param callable to execute
     * @param <T> is the return value type
     * @return Future containing the callable's future result
     */
    <T> CompletableFuture<T> execute(Callable<T> callable);
}
