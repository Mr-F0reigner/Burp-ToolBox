/*
 * Copyright (c) 2022-2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package burp.api.montoya.proxy;

/**
 * 扩展可以实现这个接口然后调用
 * {@link Proxy#history(ProxyHistoryFilter)} to get a filtered list of items in
 * the Proxy history.
 */
public interface ProxyHistoryFilter
{
    /**
     * 对代理历史记录中的每个项目调用此方法以确定
     * 是否应包含在过滤的项目列表中。
     *
     * @param requestResponse 一个 {@link ProxyHttpRequestResponse} 对象
     * 扩展可用于确定该项目是否应包含在
     * 过滤后的项目列表。
     *
     * @return 返回 {@code true} 如果该项目应该包含在
     * 过滤后的项目列表。
     */
    boolean matches(ProxyHttpRequestResponse requestResponse);
}
