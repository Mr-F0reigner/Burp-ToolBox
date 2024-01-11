package Extension.contextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import main.ToolBox;
import ui.ConfigTab;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
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

    // addSqlMapMenuItem方法添加一个名为"SQL Map"的菜单项到上下文菜单。
    public void addSqlMapMenuItem() {
        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER)) {
            JMenuItem sqlMap = new JMenuItem("SQL Map");
            sqlMap.addActionListener(e -> handleSqlMapAction());
            menuItemList.add(sqlMap);
        }
    }

    // handleSqlMapAction方法处理SQLMap菜单项的动作。
    private void handleSqlMapAction() {
        try {
            String sqlmapCMD = (String) configModel.getValueAt(0, 2);
            String filePath = extractFilePath(sqlmapCMD);
            saveRequestToFile(filePath);
            executeSqlMapCommand(sqlmapCMD);
        } catch (Exception ex) {
            api.logging().logToOutput("Error handling SQLMap action: " + ex);
        }
    }

    // extractFilePath方法从SQLMap命令中提取文件路径。
    private String extractFilePath(String command) throws IOException {
        Matcher matcher = Pattern.compile("(?<=-r ).*?\\.txt").matcher(command);
        if (matcher.find()) {
            return matcher.group();
        }
        throw new IOException("No valid file path found in command");
    }

    // saveRequestToFile方法将请求保存到文件中。
    private void saveRequestToFile(String filePath) throws IOException {
        File file = new File(filePath);
        String payload = String.valueOf(event.messageEditorRequestResponse().get().requestResponse().request());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(payload);
        }
    }

    // executeSqlMapCommand方法执行SQLMap命令。
    private void executeSqlMapCommand(String sqlmapCMD) throws IOException {
        String[] sqlmapCMDArray = sqlmapCMD.split(" ");
        String osName = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        String commandString = buildCommandString(sqlmapCMDArray);

        if (osName.contains("win")) {
            // 为Windows系统构建命令
            processBuilder = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", commandString);
        } else if (osName.contains("mac")) {
            // 为macOS系统构建命令
            processBuilder = new ProcessBuilder("osascript", "-e", "tell application \"Terminal\" to do script \"" + commandString + "\"");
        } else {
            // 为其他系统，如Linux，构建命令
            processBuilder = new ProcessBuilder("gnome-terminal", "--", "/bin/sh", "-c", commandString + "; exec bash");
        }
        // 将命令复制到剪切板
        Transferable tText = new StringSelection(commandString);
        clip.setContents(tText, null);

        processBuilder.start();
    }

    // buildCommandString方法构建用于执行的命令字符串。
    private String buildCommandString(String[] commandArray) {
        StringBuilder commandBuilder = new StringBuilder();
        for (String param : commandArray) {
            commandBuilder.append(param).append(" ");
        }
        return commandBuilder.toString().trim();
    }
}
