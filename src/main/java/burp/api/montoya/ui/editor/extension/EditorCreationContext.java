/*
 * Copyright (c) 2022-2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package burp.api.montoya.ui.editor.extension;

import burp.api.montoya.core.ToolSource;

/**
 * 该接口由一个
 * <code>ExtensionHttpRequestEditor</code> 或 <code>ExtensionHttpResponseEditor</code> 获取
 * 有关当前显示消息的详细信息。
 * 创建 Burp 的 HTTP 消息编辑器实例的扩展可以
 * 可选地提供一个实现
 * <code>IMessageEditorController</code>，编辑器在启动时将调用它
 * 需要有关当前消息的更多信息（例如，发送
 * 将其转移到另一个 Burp 工具）。通过提供自定义编辑器选项卡的扩展
 * <code>IMessageEditorTabFactory</code> 将收到对
 * 每个选项卡实例的 <code>IMessageEditorController</code> 对象
 * 生成，如果需要更多信息，选项卡可以调用它
 * 当前消息。
 */
public interface EditorCreationContext
{
    /**
     * Indicates which Burp tool is requesting the editor.
     *
     * @return The tool requesting an editor
     */
    ToolSource toolSource();

    /**
     * Indicates which modes the Burp tool requests of the editor.
     * e.g. Proxy expects a read only editor, Repeater expects the default editor.
     *
     * @return The mode required by the editor.
     */
    EditorMode editorMode();
}