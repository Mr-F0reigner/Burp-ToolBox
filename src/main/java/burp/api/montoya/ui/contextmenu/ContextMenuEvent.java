/*
 * Copyright (c) 2022-2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package burp.api.montoya.ui.contextmenu;

import burp.api.montoya.core.ToolSource;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;

import java.util.List;
import java.util.Optional;

/**
 * Provides useful information when generating context menu items from a {@link ContextMenuItemsProvider}.
 */
public interface ContextMenuEvent extends ComponentEvent, ToolSource, InvocationSource
{
    /**
     * 此方法可用于在调用上下文菜单时检索当前所选 HTTP 请求/响应的详细信息。
     *
     * @return an {@link Optional} describing the currently selected request response with selection metadata.
     */
    Optional<MessageEditorHttpRequestResponse> messageEditorRequestResponse();

    /**
     * 此方法可用于检索当前选定的 HTTP 请求/响应对的详细信息
     * 调用上下文菜单时由用户选择。如果用户尚未做出选择，这将返回一个空列表。
     *
     * @return A list of request responses that have been selected by the user.
     */
    List<HttpRequestResponse> selectedRequestResponses();

    /**
     * 此方法可用于检索用户在调用上下文菜单时选择的扫描仪问题的详细信息。
     * 如果没有问题适用于调用，这将返回一个空列表。
     *
     * @return a List of {@link AuditIssue} objects representing the items that were shown or selected by the user when the context menu was invoked.
     * @deprecated Use {@link ContextMenuItemsProvider#provideMenuItems(AuditIssueContextMenuEvent)} instead.
     */
    @Deprecated
    List<AuditIssue> selectedIssues();
}
