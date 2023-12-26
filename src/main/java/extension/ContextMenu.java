package extension;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.Proxy;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ContextMenu implements ContextMenuItemsProvider
{

    private final MontoyaApi api;

    public ContextMenu(MontoyaApi api)
    {
        this.api = api;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event)
    {
        // 设置右键菜单的作用域
        if (event.isFromTool(ToolType.PROXY,ToolType.REPEATER))
        {
            // 获取请求/响应
            HttpRequestResponse httpRequestResponse = event.messageEditorRequestResponse().get().requestResponse();
            HttpHeader cookie = httpRequestResponse.request().header("Cookie");
            HttpResponse response = httpRequestResponse.response();

            List<Component> menuItemList = new ArrayList<>();

            // 菜单项名称
            JMenuItem retrieveRequestItem = new JMenuItem("Update Certificate");

            retrieveRequestItem.addActionListener(e -> {
                // 从request中获取Host
                HttpRequest request = event.messageEditorRequestResponse().get().requestResponse().request();
                List<ProxyHttpRequestResponse> history = api.proxy().history();
                for (ProxyHttpRequestResponse item : history)
                {
                    if (item.request().hasHeader("Host"))
                    {
                        api.logging().logToOutput(item.request().header("Host").value());
                    }
                }

            });

            menuItemList.add(retrieveRequestItem);


            return menuItemList;
        }

        return null;
    }
}