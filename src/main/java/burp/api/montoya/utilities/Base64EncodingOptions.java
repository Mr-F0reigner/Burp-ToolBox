/*
 * Copyright (c) 2022-2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package burp.api.montoya.utilities;

/**
 * This enum defines HTML encodings.
 */
public enum Base64EncodingOptions
{
    /**
     * 使用 URL 和文件名安全类型 base64 转码方案进行编码
     */
    URL,

    /**
     * 编码时不在数据末尾添加任何填充字符。
     */
    NO_PADDING
}
