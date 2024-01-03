package extension;

import EditorPanel.AutorizeHttpHandler;
import EditorPanel.AutorizeTableModel;
import EditorPanel.ResponseDecoderProvider;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import contextMenu.ContextMenu;
import EditorPanel.RequestDecoderProvider;
import ui.ToolBoxUI;

import javax.swing.*;


public class ToolBox implements BurpExtension {
    public static MontoyaApi api;
    public static AutorizeTableModel autorizeTableModel = new AutorizeTableModel();
    public static ToolBoxUI toolsBoxUI;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        api = montoyaApi;
        // 插件名称
        api.extension().setName("T0o1-Bo*");

        // 打印加载成功信息
        String loadSuccess = """
                ========================================================================
                  __  __            _____ ___           _                      \s
                 |  \\/  |_ __      |  ___/ _ \\ _ __ ___(_) __ _ _ __   ___ _ __\s
                 | |\\/| | '__|     | |_ | | | | '__/ _ \\ |/ _` | '_ \\ / _ \\ '__|
                 | |  | | |     _  |  _|| |_| | | |  __/ | (_| | | | |  __/ |  \s
                 |_|  |_|_|    (_) |_|   \\___/|_|  \\___|_|\\__, |_| |_|\\___|_|  \s
                                                          |___/                \s
                [ T0o1-Bo* v1.0 ] - [ LOAD SUCCESS! ]
                - Author: Mr.F0reigner
                - GitHub: https://github.com/Mr-F0reigner/Burp-ToolBox
                ========================================================================
                """;
        api.logging().logToOutput(loadSuccess);

        // 注册GUI到Burp
        toolsBoxUI = new ToolBoxUI();
        JPanel rootPanel = toolsBoxUI.rootPanel;
        api.userInterface().registerSuiteTab("T0o1-BoX", rootPanel);
        // 注册邮件菜单
        api.userInterface().registerContextMenuItemsProvider(new ContextMenu());
        // 注册请求包解码器
        api.userInterface().registerHttpRequestEditorProvider(new RequestDecoderProvider());
        // 注册响应包解码器
        api.userInterface().registerHttpResponseEditorProvider(new ResponseDecoderProvider());

        // 注册越权检测界面
        api.http().registerHttpHandler(new AutorizeHttpHandler(autorizeTableModel,toolsBoxUI));
    }
}
