package ui;

import burp.api.montoya.MontoyaApi;
import extension.ToolBox;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigTab {
    private MontoyaApi api = ToolBox.api;
    // 配置文件初始参数
    public static String initConfig = """
            [{"Comment":"Automatic execution of SQLMAP","Value":"python.exe sqlmap.py -r SQLMapFuzz.txt --dbs --level 1","Id":"1","Key":"SQL Map"}]
            """;
    // 配置文件路径
    private static final String CONFIG_FILE_PATH = System.getProperty("user.home") + "\\" + "ToolBox.json";
    private DefaultTableModel configModel;
    private JTable configTable;
    private JScrollPane configScrollPane;

    public ConfigTab(DefaultTableModel configModel, JTable configTable, JScrollPane configScrollPane) {
        this.configModel = configModel;
        this.configTable = configTable;
        this.configScrollPane = configScrollPane;
        initConfigTable();
    }

    /**
     * 初始化Config选项卡
     * @throws IOException
     */
    private void initConfigTable() {
        // 定义列名
        final Object[] columnNames = {"#", "Key", "Value", "Comment"};
        // 使用 DefaultTableModel，解决JTable不显示问题。设置初始为空
        configModel = new DefaultTableModel(columnNames, 0);
        configTable.setModel(configModel);

        // 设置列的自动调整模式为AUTO_RESIZE_NEXT_COLUMN
        configTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // 为 JScrollPane 添加组件大小变化监听器
        configScrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustConfigColumnWidths();
            }
        });

        adjustConfigColumnWidths();

        // 为每一列设置自定义渲染器，使数据居中显示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < configTable.getColumnCount(); i++) {
            configTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        loadConfigFromFile();
    }

    /**
     * 重载插件时载入配置文件信息
     */
    private void loadConfigFromFile() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            // 如果不存在配置文件，创建并初始化配置文件
            if (!configFile.exists() || configFile.length() == 0) {
                configFile.createNewFile();
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(configFile));
                bufferedWriter.write(initConfig);
                bufferedWriter.flush();
                bufferedWriter.close();
            }

            // 读取配置文件并加载数据到burp
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE_PATH)));
            JSONArray jsonArray = new JSONArray(content);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                configModel.addRow(new Object[]{obj.getString("Id"), obj.getString("Key"), obj.getString("Value"), obj.getString("Comment")});
            }
        } catch (IOException | JSONException e) {
            api.logging().logToOutput("Error loading config: " + e.getMessage());
        }
    }

    /**
     * 保存配置文件
     */
    public void saveConfigToFile() {
        JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < configModel.getRowCount(); i++) {
            JSONObject obj = new JSONObject();
            obj.put("Id", configModel.getValueAt(i, 0));
            obj.put("Key", configModel.getValueAt(i, 1));
            obj.put("Value", configModel.getValueAt(i, 2));
            obj.put("Comment", configModel.getValueAt(i, 3));
            jsonArray.put(obj);
        }

        try (FileWriter file = new FileWriter(CONFIG_FILE_PATH)) {
            file.write(jsonArray.toString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 调整列宽。关闭Java Swing自动调整列宽锁后的列宽自适应配置
     */
    private void adjustConfigColumnWidths() {
        int totalWidth = configScrollPane.getViewport().getWidth();
        int firstColumnWidth = 50;
        int secondColumnWidth = 100;
        configTable.getColumnModel().getColumn(0).setPreferredWidth(firstColumnWidth);
        configTable.getColumnModel().getColumn(1).setPreferredWidth(secondColumnWidth);
        int remainingWidth = totalWidth - firstColumnWidth - secondColumnWidth;
        int otherColumnWidth = remainingWidth / 2; // 剩余两列平分宽度

        for (int i = 2; i < configTable.getColumnCount(); i++) {
            configTable.getColumnModel().getColumn(i).setPreferredWidth(otherColumnWidth);
        }
    }
}
