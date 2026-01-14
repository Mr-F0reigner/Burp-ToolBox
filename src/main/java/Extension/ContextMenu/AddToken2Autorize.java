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

    // [建议] 将插件的 Tab 名称定义为常量，防止拼写错误
    // 你可以在 ToolBox 类中定义 public static final String TAB_NAME = "T0o1-BoX";
    private static final String TARGET_TAB_NAME = "T0o1-BoX";

    public AddToken2Autorize(ContextMenuEvent event, List<Component> menuItemList) {
        this.event = event;
        this.menuItemList = menuItemList;
    }

    /**
     * 注册快捷键 Ctrl+Shift+Alt+A
     */
    public static void registerHotkey(MontoyaApi api) {
        HotKey hotKey = HotKey.hotKey("Add Token to Autorize", "Ctrl+Shift+Alt+A");

        HotKeyHandler handler = event -> {
            event.messageEditorRequestResponse().ifPresent(editor -> {
                try {
                    // 1. 执行核心逻辑
                    performAddTokenLogic(api, editor.requestResponse());

                    // 2. 界面跳转
                    switchToAutorizeTab();

                } catch (Exception ex) {
                    api.logging().logToError("Shortcut Action Failed: " + ex.getMessage());
                }
            });
        };

        api.userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, hotKey, handler);
    }

    /**
     * 界面跳转逻辑：向上查找直到找到目标 Tab
     */
    private static void switchToAutorizeTab() {
        SwingUtilities.invokeLater(() -> {
            if (Autorize.instance == null) {
                ToolBox.api.logging().logToError("[-] 跳转失败：Autorize 实例为空");
                return;
            }

            Component targetComp = Autorize.instance.getRootComponent();
            if (targetComp == null) {
                ToolBox.api.logging().logToError("[-] 跳转失败：无法获取 UI 根组件");
                return;
            }

            Container parent = targetComp.getParent();
            boolean found = false;
            int maxDepth = 50;
            int currentDepth = 0;

            while (parent != null && currentDepth < maxDepth) {
                if (parent instanceof JTabbedPane) {
                    JTabbedPane tabs = (JTabbedPane) parent;
                    for (int i = 0; i < tabs.getTabCount(); i++) {
                        String title = tabs.getTitleAt(i);
                        // [关键] 使用常量进行匹配
                        if (TARGET_TAB_NAME.equals(title)) {
                            tabs.setSelectedIndex(i);
                            tabs.requestFocusInWindow();
                            found = true;
                            break;
                        }
                    }
                }
                if (found) break;
                parent = parent.getParent();
                currentDepth++;
            }

            if (!found) {
                // 日志提示更明确一些
                ToolBox.api.logging().logToError("[-] 跳转失败：未找到名为 [" + TARGET_TAB_NAME + "] 的标签页。");
            }
        });
    }

    /**
     * 右键菜单入口
     */
    public void addToken() {
        // 只在 Proxy 和 Repeater 中显示，符合使用习惯
        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER)) {
            JMenuItem addTokenItem = new JMenuItem("Add Token to Autorize");
            addTokenItem.addActionListener(e -> {
                try {
                    this.findRequestAndExecute();
                } catch (Exception ex) {
                    api.logging().logToError("Menu Action Failed: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
            menuItemList.add(addTokenItem);
        }
    }

    private void findRequestAndExecute() {
        HttpRequestResponse requestResponse = null;

        List<HttpRequestResponse> selectedItems = event.selectedRequestResponses();
        if (!selectedItems.isEmpty()) {
            requestResponse = selectedItems.get(0);
        } else {
            var editorReqRes = event.messageEditorRequestResponse();
            if (editorReqRes.isPresent()) {
                requestResponse = editorReqRes.get().requestResponse();
            }
        }

        if (requestResponse == null) {
            api.logging().logToOutput("[-] 未能获取到请求包。");
            return;
        }

        performAddTokenLogic(api, requestResponse);
        // [可选] 如果你希望右键点击也跳转，可以放开下面这行注释
         switchToAutorizeTab();
    }

    /**
     * 核心逻辑：提取 Token 并更新 UI
     */
    private static void performAddTokenLogic(MontoyaApi api, HttpRequestResponse requestResponse) {
        // 1. 基础检查
        if (ConfigTab.configModel == null) {
            api.logging().logToError("[-] ConfigTab 尚未初始化。");
            return;
        }

        List<String> targetKeys = ConfigTab.getUpdateCertificateHeaders();
        // 使用更灵活的判空逻辑
        if (targetKeys == null || targetKeys.isEmpty()) {
            targetKeys = List.of("Cookie", "Authorization", "token");
        }

        // 2. 遍历提取
        List<HttpHeader> headers = requestResponse.request().headers();
        StringBuilder bypassContent = new StringBuilder();
        StringBuilder unauthContent = new StringBuilder();
        boolean found = false;

        // 双重循环提取 (由于 Header 数量通常很少，这里不用优化成 Map)
        for (HttpHeader header : headers) {
            for (String key : targetKeys) {
                if (header.name().equalsIgnoreCase(key)) {
                    bypassContent.append(header.toString()).append("\n");
                    unauthContent.append(header.name()).append("\n");
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            api.logging().logToOutput("[-] 未找到目标字段: " + targetKeys);
            return;
        }

        if (Autorize.instance == null) {
            api.logging().logToError("[-] Autorize 实例未找到。");
            return;
        }

        // 3. 更新 UI
        SwingUtilities.invokeLater(() -> {
            Autorize.instance.updateAuthBypassContent(bypassContent.toString());
            Autorize.instance.updateUnauthContent(unauthContent.toString());
        });

        api.logging().logToOutput("[+] Token 更新成功 -> Autorize");
    }
}