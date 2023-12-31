/*
 * Copyright (c) 2022-2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package burp.api.montoya.http.message.params;

import burp.api.montoya.core.Range;

/**
 * Burp {@link HttpParameter} 包含有关已由 Burp 解析的 HTTP 请求参数的其他详细信息。
 */
public interface ParsedHttpParameter extends HttpParameter
{
    /**
     * @return The parameter type.
     */
    @Override
    HttpParameterType type();

    /**
     * @return The parameter name.
     */
    @Override
    String name();

    /**
     * @return The parameter value.
     */
    @Override
    String value();

    /**
     * HTTP 请求中参数名称的偏移量。
     *
     * @return The parameter name offsets.
     */
    Range nameOffsets();

    /**
     * Offsets of the parameter value within the HTTP request.
     *
     * @return The parameter value offsets.
     */
    Range valueOffsets();
}
