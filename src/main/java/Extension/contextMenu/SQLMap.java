package Extension.contextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import main.ToolBox;
import ui.ConfigTab;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLMap {
    private MontoyaApi api = ToolBox.api;
    private ContextMenuEvent event;
    private List<Component> menuItemList;
    private DefaultTableModel configModel = ConfigTab.configModel;

    public SQLMap(ContextMenuEvent event, List<Component> menuItemList) {
        this.event = event;
        this.menuItemList = menuItemList;
    }

    public void SQLMap() {
        // 添加 SQLMap 菜单项
        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER)) {
            JMenuItem sqlMap = new JMenuItem("SQL Map");
            sqlMap.addActionListener(e -> handleSqlMapAction());
            menuItemList.add(sqlMap);
        }
    }

    private void handleSqlMapAction() {
        try {
            String sqlmapCMD = (String) configModel.getValueAt(0, 2);
            String filePath = extractFilePath(sqlmapCMD);
            saveRequestToFile(filePath);
            executeSqlMapCommand(sqlmapCMD);
        } catch (Exception ex) {
            api.logging().logToOutput("Error handling SQLMap action: " + ex.getMessage());
        }
    }

    private String extractFilePath(String command) throws IOException {
        Matcher matcher = Pattern.compile("(?<=-r ).*?\\.txt").matcher(command);
        if (matcher.find()) {
            return matcher.group();
        }
        throw new IOException("No valid file path found in command");
    }

    private void saveRequestToFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }

        String payload = String.valueOf(event.messageEditorRequestResponse().get().requestResponse().request());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(payload);
        }
    }

    private void executeSqlMapCommand(String sqlmapCMD) throws IOException {
        String[] sqlmapCMDArray = sqlmapCMD.split(" ");
        String osName = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        if (osName.contains("win")) {
            // Windows 系统
            StringBuilder commandBuilder = new StringBuilder();
            for (String param : sqlmapCMDArray) {
                commandBuilder.append(param).append(" ");
            }
            String commandString = commandBuilder.toString().trim();
            processBuilder = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", commandString);
        } else if (osName.contains("mac")) {
            // macOS 系统
            StringBuilder commandBuilder = new StringBuilder();
            commandBuilder.append("tell application \"Terminal\" to do script \"");
            for (String param : sqlmapCMDArray) {
                commandBuilder.append(param).append(" ");
            }
            commandBuilder.append("\"");
            String commandString = commandBuilder.toString();
            processBuilder = new ProcessBuilder("osascript", "-e", commandString);
        } else {
            // 非 Windows 或 macOS 系统，例如 Linux
            StringBuilder commandBuilder = new StringBuilder();
            for (String param : sqlmapCMDArray) {
                commandBuilder.append(param).append(" ");
            }
            String commandString = commandBuilder.toString().trim();
            // "; exec bash" 防止执行完毕后自动退出
            processBuilder = new ProcessBuilder("gnome-terminal", "--", "/bin/sh", "-c", commandString + "; exec bash");
        }
        processBuilder.start();
    }
}
