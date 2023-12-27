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
        String loadSuccess = """
                ========================================================================
                  __  __            _____ ___           _                      \s
                 |  \\/  |_ __      |  ___/ _ \\ _ __ ___(_) __ _ _ __   ___ _ __\s
                 | |\\/| | '__|     | |_ | | | | '__/ _ \\ |/ _` | '_ \\ / _ \\ '__|
                 | |  | | |     _  |  _|| |_| | | |  __/ | (_| | | | |  __/ |  \s
                 |_|  |_|_|    (_) |_|   \\___/|_|  \\___|_|\\__, |_| |_|\\___|_|  \s
                                                          |___/                \s
                [ T0ol5-BoX v1.0 ] - [ LOAD SUCCESS! ]
                - Author: Mr.F0reigner
                - GitHub: https://github.com/Mr-F0reigner/BurpExtension-ToolsBox
                ========================================================================
                """;
        api.logging().logToOutput(loadSuccess);
        api.userInterface().registerContextMenuItemsProvider(new ContextMenu());
    }
}
