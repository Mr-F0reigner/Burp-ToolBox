package contextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import extension.ToolBox;
import ui.ToolBoxUI;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SQLMap {
    private MontoyaApi api = ToolBox.api;
    private ContextMenuEvent event;
    private List<Component> menuItemList;
    private ArrayList<String> originalConfigData = ToolBoxUI.originalConfigData;
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
                    // 使用空格分割 payload 字符串
                    String[] payloadParts = originalConfigData.get(0).split(" ");

                    // 获取用户桌面\SqlMapTemP.txt路径
                    String filePath = "";
                    Pattern pattern = Pattern.compile(".*\\.txt$");
                    for (String part : payloadParts) {
                        Matcher matcher = pattern.matcher(part);
                        if (matcher.matches()) {
                            filePath = part;
                        }
                    }

                    // 判断文件是否存在
                    File file = new File(filePath);
                    if(!file.exists()) {
                        file.createNewFile();
                    }

                    // 保存请求包到文件中
                    String payload = String.valueOf(event.messageEditorRequestResponse().get().requestResponse().request());
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                    bufferedWriter.write(payload);
                    bufferedWriter.flush();
                    bufferedWriter.close();

                    // 创建完整的命令数组，长度为 payloadParts.length + 5（因为有5个额外的命令部分）
                    String[] command = new String[payloadParts.length + 5];
                    command[0] = "cmd";
                    command[1] = "/c";
                    command[2] = "start";
                    command[3] = "cmd";
                    command[4] = "/k";
                    System.arraycopy(payloadParts, 0, command, 5, payloadParts.length);

                    // 创建并启动进程
                    ProcessBuilder builder = new ProcessBuilder(command);
                    builder.start();
                } catch (IOException ioException) {
                    api.logging().logToOutput(ioException.getMessage());
                }
            });
            menuItemList.add(sqlMap);
        }
    }
}
