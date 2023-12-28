package contextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import extension.ToolBox;
import contextMenu.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class ContextMenu implements ContextMenuItemsProvider {
    public MontoyaApi api = ToolBox.api;
    public static JMenuItem UpdateCertificate;

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        // 创建右键菜单列表
        List<Component> menuItemList = new ArrayList<>();

        // 凭证更新
        UpdateCertificate updateCertificate = new UpdateCertificate(event,menuItemList);
        updateCertificate.UpdateCertificate();

        // SQLMap
        SQLMap sqlMap = new SQLMap(event,menuItemList);
        sqlMap.SQLMap();

        return menuItemList;
    }
}