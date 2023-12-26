/*
 * Copyright (c) 2022-2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package burp.api.montoya.proxy.http;

import burp.api.montoya.proxy.Proxy;

/**
 * Extensions can implement this interface and then call
 * {@link Proxy#registerRequestHandler(ProxyRequestHandler)} to register a
 * Proxy request handler. The handler will be notified of requests being
 * processed by the Proxy tool. Extensions can perform custom analysis or
 * modification of these messages, and control in-UI message interception.
 */
public interface ProxyRequestHandler
{
    /**
     * 该方法在代理接收到 HTTP 请求之前调用。<br>
     * 可以修改请求。<br>
     * 可以修改注释。<br>
     * 可以控制请求是否应该被拦截并显示给用户以供手动审核或修改。<br>
     * 可以放弃请求。<br>
     *
     * @param interceptedRequest An {@link InterceptedRequest} object that extensions can use to query and update details of the request.
     *
     * @return The {@link ProxyRequestReceivedAction} containing the required action, annotations and HTTP request to be passed through the proxy.
     */
    ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest);

    /**
     * This method is invoked after an HTTP request has been processed by the Proxy before it is sent.<br>
     * Can modify the request.<br>
     * Can modify the annotations.<br>
     * Can control whether the request is sent or dropped.<br>
     *
     * @param interceptedRequest An {@link InterceptedRequest} object that extensions can use to query and update details of the intercepted request.
     *
     * @return The {@link ProxyRequestToBeSentAction} containing the required action, annotations and HTTP request to be sent from the proxy.
     */
    ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest);
}
