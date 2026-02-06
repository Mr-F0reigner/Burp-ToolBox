package Extension.ContextMenu;

import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.http.message.HttpRequestResponse;
import ui.Autorize;

import javax.swing.JMenuItem;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public class AutorizeURLFilter implements ContextMenuItemsProvider {

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuList = new ArrayList<>();

        // 尝试获取当前选中的请求
        HttpRequestResponse selectedRequest = null;

        // 1. 优先从消息编辑器（如 Repeater, Proxy 详情页）获取
        if (event.messageEditorRequestResponse().isPresent()) {
            selectedRequest = event.messageEditorRequestResponse().get().requestResponse();
        }
        // 2. 其次从列表选择（如 Proxy History 列表）获取
        else if (!event.selectedRequestResponses().isEmpty()) {
            selectedRequest = event.selectedRequestResponses().get(0);
        }

        // 只有当选中了请求时，才创建这个菜单项
        if (selectedRequest != null) {
            // 保存 final 变量供 Lambda 使用
            final HttpRequestResponse finalReqRes = selectedRequest;

            // 这个名字 "Add to Autorize Filter" 就是你在 Burp 设置里搜索的名字
            JMenuItem addItem = new JMenuItem("Add to Autorize Filter");

            addItem.addActionListener(e -> {
                // 调用 Autorize 单例的方法
                if (Autorize.instance != null) {
                    // 获取 URL
                    String url = finalReqRes.request().url();
                    // 执行添加逻辑
                    Autorize.instance.addUrlToFilter(url);
                }
            });

            menuList.add(addItem);
        }

        return menuList;
    }
}