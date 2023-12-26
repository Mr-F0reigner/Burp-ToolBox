package extension;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import ui.ToolsBoxUI;

import javax.swing.*;


public class ToolsBox implements BurpExtension {
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        // 插件名称
        montoyaApi.extension().setName("T0ol5-BoX");
        // 注册GUI到Burp
        ToolsBoxUI toolsBoxUI = new ToolsBoxUI();
        JPanel rootPanel = toolsBoxUI.rootPanel;
        montoyaApi.userInterface().registerSuiteTab("T0ol5-BoX",rootPanel);
        // 打印加载成功信息
        montoyaApi.logging().logToOutput("~~Successfully loaded~~");
        montoyaApi.userInterface().registerContextMenuItemsProvider(new ContextMenu(montoyaApi));
    }
}
