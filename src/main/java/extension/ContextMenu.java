package extension;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
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
                api.logging().logToOutput(cookie.value());
                api.logging().logToOutput(String.valueOf(response));
            });

            menuItemList.add(retrieveRequestItem);


            return menuItemList;
        }

        return null;
    }
}