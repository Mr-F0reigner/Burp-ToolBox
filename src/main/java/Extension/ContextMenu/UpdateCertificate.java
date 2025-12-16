package Extension.ContextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import burp.api.montoya.ui.hotkey.HotKey;
import burp.api.montoya.ui.hotkey.HotKeyContext; // [新增] 引入 HotKeyContext
import burp.api.montoya.ui.hotkey.HotKeyHandler; // [新增] 引入 HotKeyHandler
import main.ToolBox;
import ui.ConfigTab;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class UpdateCertificate {
    private MontoyaApi api = ToolBox.api;
    private ContextMenuEvent event;
    private List<Component> menuItemList;

    // 静态资源文件扩展名模式
    private static final Pattern STATIC_RESOURCE_PATTERN =
            Pattern.compile("\\.(js|css|gif|jpg|jpeg|png|ico|svg|woff|woff2|ttf|eot|map)(\\?.*)?$", Pattern.CASE_INSENSITIVE);

    public UpdateCertificate(ContextMenuEvent event, List<Component> menuItemList) {
        this.event = event;
        this.menuItemList = menuItemList;
    }

    /**
     * [新增] 静态方法：在插件初始化时调用此方法注册快捷键
     * 请在你的 BurpExtension.initialize() 方法中调用:
     * UpdateCertificate.registerHotkey(api);
     */
    public static void registerHotkey(MontoyaApi api) {
        // 定义快捷键和名称 (名称会显示在命令面板中)
        HotKey hotKey = HotKey.hotKey("Update Certificate", "Ctrl+Shift+Alt+U");

        // 定义处理器
        HotKeyHandler handler = event -> {
            // 获取当前编辑器 (如果快捷键是在编辑器上下文中按下的)
            event.messageEditorRequestResponse().ifPresent(editor -> {
                // 调用抽取的公共逻辑
                performUpdateCertificateLogic(api, editor);
            });
        };

        // 注册到 HTTP 消息编辑器上下文 (Proxy, Repeater 等编辑器中生效)
        api.userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, hotKey, handler);
    }

    /**
     * 右键菜单入口
     */
    public void UpdateCertificate() {
        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER)) {
            JMenuItem updateCertificate = new JMenuItem("Update Certificate");

            updateCertificate.addActionListener(e -> {
                performUpdateCertificate();
            });

            menuItemList.add(updateCertificate);
        }
    }

    /**
     * 实例方法：适配旧的右键菜单逻辑
     */
    private void performUpdateCertificate() {
        if (event == null || event.messageEditorRequestResponse().isEmpty()) {
            api.logging().logToError("No message editor available");
            return;
        }
        // 调用公共逻辑
        performUpdateCertificateLogic(api, event.messageEditorRequestResponse().get());
    }

    /**
     * [重构] 核心逻辑抽取：不再依赖实例变量 event，改为传入 editor
     * 这样既可以被 Hotkey 调用，也可以被 ContextMenu 调用
     */
    private static void performUpdateCertificateLogic(MontoyaApi api, MessageEditorHttpRequestResponse editor) {
        try {
            HttpRequest currentRequest = editor.requestResponse().request();
            if (currentRequest == null) {
                api.logging().logToError("Current request is null");
                return;
            }

            String currentHost = currentRequest.headerValue("Host");
            if (currentHost == null || currentHost.isEmpty()) {
                api.logging().logToOutput("No Host header found");
                return;
            }

            // 获取历史记录
            List<ProxyHttpRequestResponse> history = api.proxy().history();
            if (history.isEmpty()) {
                api.logging().logToOutput("No history available");
                return;
            }

            // 执行凭证更新
            updateCredentialsFromHistory(api, editor, currentRequest, currentHost, history);

        } catch (Exception ex) {
            api.logging().logToError("UpdateCertificate error: " + ex.getMessage());
        }
    }

    private static void updateCredentialsFromHistory(MontoyaApi api, MessageEditorHttpRequestResponse editor,
                                                     HttpRequest currentRequest, String targetHost,
                                                     List<ProxyHttpRequestResponse> history) {
        // 从配置获取需要更新的凭证字段
        List<String> targetHeaders = ConfigTab.getUpdateCertificateHeaders();
        api.logging().logToOutput("Target headers to update: " + String.join(", ", targetHeaders));

        // 存储每个字段的最新值
        Map<String, String> latestHeaders = new HashMap<>();
        int foundCount = 0;

        // 从最新的记录开始向前遍历（最多30条）
        int startIndex = Math.max(0, history.size() - 1);
        int endIndex = Math.max(0, history.size() - 30);

        for (int i = startIndex; i >= endIndex && foundCount < targetHeaders.size(); i--) {
            ProxyHttpRequestResponse historyItem = history.get(i);
            if (historyItem == null || historyItem.request() == null) {
                continue;
            }

            // 检查主机是否匹配
            String itemHost = historyItem.request().headerValue("Host");
            if (!targetHost.equals(itemHost)) {
                continue;
            }

            // 检查是否为静态资源请求
            if (isStaticResource(historyItem.request().url())) {
                continue;
            }

            // 提取凭证信息
            for (String header : targetHeaders) {
                if (!latestHeaders.containsKey(header) && historyItem.request().hasHeader(header)) {
                    String headerValue = historyItem.request().headerValue(header);
                    latestHeaders.put(header, headerValue);
                    foundCount++;
                    api.logging().logToOutput("Found " + header + " in history item: " + i);

                    if (foundCount >= targetHeaders.size()) {
                        break;
                    }
                }
            }
        }

        // 应用更新
        boolean updated = false;
        HttpRequest updatedRequest = currentRequest;

        for (Map.Entry<String, String> entry : latestHeaders.entrySet()) {
            String headerName = entry.getKey();
            String headerValue = entry.getValue();

            if (updatedRequest.hasHeader(headerName)) {
                updatedRequest = updatedRequest.withUpdatedHeader(headerName, headerValue);
            } else {
                updatedRequest = updatedRequest.withAddedHeader(headerName, headerValue);
            }
            updated = true;
            api.logging().logToOutput("Updated " + headerName + " header");
        }

        if (updated) {
            // [修改] 使用传入的 editor 设置请求
            editor.setRequest(updatedRequest);
            api.logging().logToOutput("Credentials updated successfully from history");
        } else {
            api.logging().logToOutput("No valid credentials found in recent history for target headers: " +
                    String.join(", ", targetHeaders));
        }
    }

    /**
     * 检查URL是否为静态资源 (静态方法)
     */
    private static boolean isStaticResource(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return STATIC_RESOURCE_PATTERN.matcher(url).find();
    }
}