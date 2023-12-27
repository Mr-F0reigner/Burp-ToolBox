/*
 * Copyright (c) 2022-2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package burp.api.montoya.ui.swing;

import burp.api.montoya.core.HighlightColor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;

/**
 * 该界面使您可以访问 swing 实用程序。
 */
public interface SwingUtils
{
    /**
     * @return 主 Burp 套件框架。
     */
    Frame suiteFrame();

    /**
     * 检索包含所提供组件的顶级{@code Window}。
     *
     * @param 组件 组件。
     *
     * @return 包含组件的顶级{@code Window}。
     */
    Window windowForComponent(Component component);

    /**
     * 将突出显示颜色转换为 java 颜色。
     *
     * @paramhighlightColor {@linkHighlightColor}
     *
     * @return 突出显示颜色的java颜色。
     */
    Color colorForHighLight(HighlightColor highlightColor);
}
