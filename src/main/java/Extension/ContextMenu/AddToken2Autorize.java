package Extension.ContextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.hotkey.HotKey;
import burp.api.montoya.ui.hotkey.HotKeyContext;
import burp.api.montoya.ui.hotkey.HotKeyHandler;
import main.ToolBox;
import ui.Autorize;
import ui.ConfigTab;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AddToken2Autorize {
    private MontoyaApi api = ToolBox.api;
    private ContextMenuEvent event;
    private List<Component> menuItemList;

    public AddToken2Autorize(ContextMenuEvent event, List<Component> menuItemList) {
        this.event = event;
        this.menuItemList = menuItemList;
    }

    /**
     * [新增] 静态方法：注册快捷键 Ctrl+Alt+Shift+A
     * 请在 BurpExtension.initialize() 中调用: AddToken2Autorize.registerHotkey(api);
     */
    public static void registerHotkey(MontoyaApi api) {
        // 定义快捷键
        HotKey hotKey = HotKey.hotKey("Add Token to Autorize", "Ctrl+Shift+Alt+A");

        // 定义处理器
        HotKeyHandler handler = event -> {
            // 获取当前编辑器内容 (快捷键只在编辑器上下文中生效)
            event.messageEditorRequestResponse().ifPresent(editor -> {
                try {
                    // 调用公共逻辑，传入 API 和 当前请求响应对象
                    performAddTokenLogic(api, editor.requestResponse());
                } catch (Exception ex) {
                    api.logging().logToError("Shortcut Action Failed: " + ex.getMessage());
                }
            });
        };

        // 注册到 HTTP 消息编辑器上下文
        api.userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, hotKey, handler);
    }

    /**
     * 右键菜单入口
     */
    public void addToken() {
        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER)) {
            JMenuItem addTokenItem = new JMenuItem("Add Token to Autorize");
            addTokenItem.addActionListener(e -> {
                try {
                    // 调用实例方法寻找请求，然后转交公共逻辑
                    this.findRequestAndExecute();
                } catch (Exception ex) {
                    api.logging().logToError("Menu Action Failed: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
            menuItemList.add(addTokenItem);
        }
    }

    /**
     * 实例方法：负责从 ContextMenuEvent 中寻找请求包 (兼容列表选中和编辑器)
     * 找到后调用静态公共逻辑
     */
    private void findRequestAndExecute() {
        HttpRequestResponse requestResponse = null;

        // 1. 尝试从列表选中项获取
        List<HttpRequestResponse> selectedItems = event.selectedRequestResponses();
        if (!selectedItems.isEmpty()) {
            requestResponse = selectedItems.get(0);
        }
        // 2. 尝试从编辑器上下文获取
        else {
            var editorReqRes = event.messageEditorRequestResponse();
            if (editorReqRes.isPresent()) {
                requestResponse = editorReqRes.get().requestResponse();
            }
        }

        if (requestResponse == null) {
            api.logging().logToOutput("[-] 未能获取到请求包。");
            return;
        }

        // 调用静态公共逻辑
        performAddTokenLogic(api, requestResponse);
    }

    /**
     * [重构] 核心逻辑抽取 (静态方法)
     * 不依赖 ContextMenuEvent，只依赖传入的 HttpRequestResponse
     */
    private static void performAddTokenLogic(MontoyaApi api, HttpRequestResponse requestResponse) {
        api.logging().logToOutput("[*] 开始执行 AddToken2Autorize...");

        // 1. 基础检查
        if (ConfigTab.configModel == null) {
            api.logging().logToError("[-] ConfigTab 尚未初始化。");
            return;
        }

        List<String> targetKeys = ConfigTab.getUpdateCertificateHeaders();
        if (targetKeys == null || targetKeys.isEmpty()) {
            targetKeys = List.of("Cookie", "Authorization", "token");
        }

        // 2. 遍历提取
        List<HttpHeader> headers = requestResponse.request().headers();
        StringBuilder bypassContent = new StringBuilder(); // Name: Value
        StringBuilder unauthContent = new StringBuilder(); // Name only
        boolean found = false;

        for (HttpHeader header : headers) {
            for (String key : targetKeys) {
                if (header.name().equalsIgnoreCase(key)) {
                    // Bypass: Name: Value
                    bypassContent.append(header.toString()).append("\n");
                    // Unauth: Name only
                    unauthContent.append(header.name()).append("\n");

                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            api.logging().logToOutput("[-] 当前请求包中未找到配置的凭证字段: " + targetKeys.toString());
            return;
        }

        // 3. Autorize 实例检查
        if (Autorize.instance == null) {
            api.logging().logToError("[-] Autorize 实例未找到。");
            return;
        }

        // 4. 更新 UI 和后台数据
        SwingUtilities.invokeLater(() -> {
            Autorize.instance.updateAuthBypassContent(bypassContent.toString());
            Autorize.instance.updateUnauthContent(unauthContent.toString());
        });

        api.logging().logToOutput("[+] 成功更新 Autorize (Bypass & Unauth)");
    }
}