/*
 * Copyright (c) 2022-2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package burp.api.montoya.proxy;

import burp.api.montoya.core.Registration;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyResponseHandler;
import burp.api.montoya.proxy.websocket.ProxyWebSocketCreationHandler;

import java.util.List;

/**
 * 提供对代理工具功能的访问。
 */
public interface Proxy
{
    /**
     * 该方法启用Burp Proxy的master拦截。
     */
    void enableIntercept();

    /**
     * 该方法禁用Burp Proxy的master拦截。
     */
    void disableIntercept();

    /**
     * 此方法返回代理 HTTP 历史记录中所有项目的详细信息。
     *
     * @return 中所有 {@link ProxyHttpRequestResponse} 项的列表
     * 代理 HTTP 历史记录。
     */
    List<ProxyHttpRequestResponse> history();

    /**
     * 此方法根据代理 HTTP 历史记录返回项目的详细信息
     * 过滤器。
     *
     * @param filter 可以使用的{@link ProxyHistoryFilter}实例
     * 过滤代理历史记录中的项目。
     *
     * @return 代理中的{@link ProxyHttpRequestResponse}项列表
     * 与过滤器匹配的 HTTP 历史记录。
     */
    List<ProxyHttpRequestResponse> history(ProxyHistoryFilter filter);

    /**
     * 此方法返回代理 WebSockets 历史记录中所有项目的详细信息。
     *
     * @return 中所有 {@link ProxyWebSocketMessage} 项的列表
     * 代理 WebSocket 历史记录。
     */
    List<ProxyWebSocketMessage> webSocketHistory();

    /**
     * 此方法返回基于代理 WebSockets 历史记录的项目的详细信息
     * 在过滤器上。
     *
     * @param filter 可以使用的{@link ProxyWebSocketHistoryFilter}实例
     * 过滤代理 WebSockets 历史记录中的项目。
     *
     * @return 代理 WebSocket 中的 {@link ProxyWebSocketMessage} 项列表
     * 与过滤器匹配的历史记录。
     */
    List<ProxyWebSocketMessage> webSocketHistory(ProxyWebSocketHistoryFilter filter);

    /**
     * 注册一个处理程序，该处理程序将被通知
     * 代理工具正在处理的请求。扩展可以执行
     * 自定义分析或修改这些消息，并在UI中进行控制
     * 消息拦截。
     *
     * @param handler 由扩展创建的对象，该对象实现了
     * {@link ProxyRequestHandler} 接口。
     *
     * @return 处理程序的{@link Registration}。
     */
    Registration registerRequestHandler(ProxyRequestHandler handler);

    /**
     * 注册一个处理程序，该处理程序将被通知
     * 代理工具正在处理响应。扩展可以执行
     * 自定义分析或修改这些消息，并在UI中进行控制
     * 消息拦截。
     *
     * @param handler 由扩展创建的对象，该对象实现了
     * {@link ProxyResponseHandler} 接口。
     *
     * @return 处理程序的{@link Registration}。
     */
    Registration registerResponseHandler(ProxyResponseHandler handler);

    /**
     * 注册一个处理程序，每当代理工具创建 WebSocket 时都会调用该处理程序。
     *
     * @param handler 由实现 {@link ProxyWebSocketCreationHandler} 接口的扩展创建的对象。
     *
     * @return 处理程序的{@link Registration}。
     */
    Registration registerWebSocketCreationHandler(ProxyWebSocketCreationHandler handler);
}
