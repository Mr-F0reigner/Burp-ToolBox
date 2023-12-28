package contextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import extension.ToolBox;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class UpdateCertificate {
    private MontoyaApi api = ToolBox.api;
    private ContextMenuEvent event;
    private List<Component> menuItemList;

    public UpdateCertificate(ContextMenuEvent event,List<Component> menuItemList) {
        this.event = event;
        this.menuItemList = menuItemList;
    }

    public void UpdateCertificate() {
        // 设置右键菜单的作用域
        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER)) {
            // 菜单项名称
            JMenuItem updateCertificate = new JMenuItem("Update Certificate");
            // 添加点击事件
            updateCertificate.addActionListener(e -> {
                // 获取执行插件的请求数据包
                HttpRequest currentRequest = event.messageEditorRequestResponse().get().requestResponse().request();
                // 获取当前主机
                String currentHost = currentRequest.headerValue("Host");

                // 获取所有history
                List<ProxyHttpRequestResponse> history = api.proxy().history();

                // 处理最新的30个数据包，不满30则全部处理
                int historySize = history.size();
                int startIndex = historySize > 30 ? historySize - 30 : 0;
                for (int i = startIndex; i < historySize; i++) {
                    ProxyHttpRequestResponse item = history.get(i);
                    // 替换Cookie字段
                    if (item.request().headerValue("Host").equals(currentHost) && item.request().hasHeader("Cookie")) {
                        HttpRequest newCookie = currentRequest.withHeader("Cookie", item.request().headerValue("Cookie"));
                        event.messageEditorRequestResponse().get().setRequest(newCookie);
                    }
                    // 替换Authorization字段
                    if (item.request().headerValue("Host").equals(currentHost) && item.request().hasHeader("Authorization")) {
                        HttpRequest newAuthorization = currentRequest.withHeader("Authorization", item.request().headerValue("Authorization"));
                        event.messageEditorRequestResponse().get().setRequest(newAuthorization);
                    }
                }
            });
            menuItemList.add(updateCertificate);
        }
    }
}