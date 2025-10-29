package Extension.ContextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import main.ToolBox;
import ui.ConfigTab; // 添加这个导入

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

    public void UpdateCertificate() {
        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER)) {
            // 创建菜单项，不设置快捷键
            JMenuItem updateCertificate = new JMenuItem("Update Certificate");

            // 只保留点击事件
            updateCertificate.addActionListener(e -> {
                performUpdateCertificate();
            });

            menuItemList.add(updateCertificate);
        }
    }

    private void performUpdateCertificate() {
        try {
            // 安全检查
            if (event == null || event.messageEditorRequestResponse().isEmpty()) {
                api.logging().logToError("No message editor available");
                return;
            }

            HttpRequest currentRequest = event.messageEditorRequestResponse().get().requestResponse().request();
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
            updateCredentialsFromHistory(currentRequest, currentHost, history);

        } catch (Exception ex) {
            api.logging().logToError("UpdateCertificate error: " + ex.getMessage());
        }
    }

    private void updateCredentialsFromHistory(HttpRequest currentRequest, String targetHost,
                                              List<ProxyHttpRequestResponse> history) {
        // 从配置获取需要更新的凭证字段
        List<String> targetHeaders = ConfigTab.getUpdateCertificateHeaders(); // 调用配置方法
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

            // 提取凭证信息 - 使用配置的字段
            for (String header : targetHeaders) {
                if (!latestHeaders.containsKey(header) && historyItem.request().hasHeader(header)) {
                    String headerValue = historyItem.request().headerValue(header);
                    latestHeaders.put(header, headerValue);
                    foundCount++;
                    api.logging().logToOutput("Found " + header + " in history item: " + i);

                    // 如果所有目标头部都已找到，提前退出
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
            event.messageEditorRequestResponse().get().setRequest(updatedRequest);
            api.logging().logToOutput("Credentials updated successfully from history");
        } else {
            api.logging().logToOutput("No valid credentials found in recent history for target headers: " +
                    String.join(", ", targetHeaders));
        }
    }

    /**
     * 检查URL是否为静态资源
     */
    private boolean isStaticResource(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return STATIC_RESOURCE_PATTERN.matcher(url).find();
    }
}