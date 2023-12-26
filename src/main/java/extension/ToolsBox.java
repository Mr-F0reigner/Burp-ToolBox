package extension;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import ui.ToolsBoxUI;

import javax.swing.*;


public class ToolsBox implements BurpExtension {
    public static MontoyaApi api;
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        api = montoyaApi;
        // 插件名称
        api.extension().setName("T0ol5-BoX");
        // 注册GUI到Burp
        ToolsBoxUI toolsBoxUI = new ToolsBoxUI();
        JPanel rootPanel = toolsBoxUI.rootPanel;
        api.userInterface().registerSuiteTab("T0ol5-BoX",rootPanel);
        // 打印加载成功信息
        api.logging().logToOutput("~~Successfully loaded~~");
        api.userInterface().registerContextMenuItemsProvider(new ContextMenu(api));
    }
}
