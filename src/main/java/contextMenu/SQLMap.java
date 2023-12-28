package contextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import extension.ToolBox;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;

public class SQLMap {
    private MontoyaApi api = ToolBox.api;
    private ContextMenuEvent event;
    private List<Component> menuItemList;

    public SQLMap(ContextMenuEvent event, List<Component> menuItemList) {
        this.event = event;
        this.menuItemList = menuItemList;
    }

    public void SQLMap() {
        // 设置右键菜单的作用域
        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER)) {
            JMenuItem sqlMap = new JMenuItem("SQL Map");
            sqlMap.addActionListener(e -> {
                try {
                    // 保存请求包到文件中
                    String payload = String.valueOf(event.messageEditorRequestResponse().get().requestResponse().request());
                    api.logging().logToOutput(String.valueOf(event.messageEditorRequestResponse().get().requestResponse().request()));
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("C:\\Users\\Xi_xi\\Desktop\\temp.txt"));
                    bufferedWriter.write(payload);
                    bufferedWriter.flush();
                    bufferedWriter.close();
                    // 创建 ProcessBuilder 实例
                    ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "start", "cmd.exe", "/k", "C:\\Users\\Xi_xi\\AppData\\Local\\Programs\\Python\\Python37\\python.exe", "D:\\PT_Tools\\SQLMap\\sqlmap.py", "-r", "C:\\Users\\Xi_xi\\Desktop\\temp.txt", "--level", "1");
                    builder.redirectErrorStream(true); // 将错误输出和标准输出合并

                    // 启动进程
                    Process process = builder.start();
                } catch (IOException ioException) {
                    api.logging().logToOutput(ioException.getMessage());
                }
            });
            menuItemList.add(sqlMap);
        }
    }
}
