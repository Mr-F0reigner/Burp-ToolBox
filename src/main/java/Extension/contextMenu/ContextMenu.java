package Extension.contextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import main.ToolBox;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class ContextMenu implements ContextMenuItemsProvider {
    public MontoyaApi api = ToolBox.api;

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        // 创建右键菜单列表
        List<Component> menuItemList = new ArrayList<>();

        // 凭证更新
        UpdateCertificate updateCertificate = new UpdateCertificate(event,menuItemList);
        updateCertificate.UpdateCertificate();

        // SQLMap
        SQLMap sqlMap = new SQLMap(event,menuItemList);
        sqlMap.addSqlMapMenuItem();

        return menuItemList;
    }
}