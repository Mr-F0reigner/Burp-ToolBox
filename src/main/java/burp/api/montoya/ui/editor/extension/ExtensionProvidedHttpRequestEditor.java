/*
 * Copyright (c) 2022-2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package burp.api.montoya.ui.editor.extension;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.Selection;

import java.awt.Component;

/**
 * Extensions that register an {@link HttpRequestEditorProvider} must return an instance of this interface.<br/>
 * Burp will then use that instance to create custom tabs within its HTTP request editor.
 */
public interface ExtensionProvidedHttpRequestEditor extends ExtensionProvidedEditor
{
    /**
     * @return 从 HTTP 请求编辑器的内容派生的 {@link HttpRequest} 实例。
     */
    HttpRequest getRequest();

   /**
     * 在编辑器组件中设置提供的 {@link HttpRequestResponse} 对象。
     *
     * @param requestResponse 要在编辑器中设置的请求和响应。
     */
    @Override
    void setRequestResponse(HttpRequestResponse requestResponse);

    /**
     * A check to determine if the HTTP message editor is enabled for a specific {@link HttpRequestResponse}
     *
     * @param requestResponse The {@link HttpRequestResponse} to check.
     *
     * @return True if the HTTP message editor is enabled for the provided request and response.
     */
    @Override
    boolean isEnabledFor(HttpRequestResponse requestResponse);

    /**
     * @return The caption located in the message editor tab header.
     */
    @Override
    String caption();

    /**
     * @return The component that is rendered within the message editor tab.
     */
    @Override
    Component uiComponent();

    /**
     * The method should return {@code null} if no data has been selected.
     *
     * @return The data that is currently selected by the user.
     */
    @Override
    Selection selectedData();

    /**
     * @return True if the user has modified the current message within the editor.
     */
    @Override
    boolean isModified();
}
