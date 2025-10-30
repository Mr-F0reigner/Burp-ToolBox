package ui;

import burp.api.montoya.MontoyaApi;
import main.ToolBox;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigTab {
    private MontoyaApi api = ToolBox.api;
    public static DefaultTableModel configModel;
    // 配置文件初始参数
    public static String initConfig = """
        [
            {"Id":"1","Key":"SQL Map","Value":"python.exe sqlmap.py -r SQLMapFuzz.txt --dbs --level 1","Comment":"一键SQLMap 填写绝对路径(包含空格需要用双引号引用)"},
            {"Id":"2","Key":"Update Certificate","Value":"Cookie,Authorization,token","Comment":"Update Certificate 需要更新的凭证字段，用逗号分隔"}
        ]
        """;
    // 配置文件路径
    private static String CONFIG_FILE_PATH;
    private JTable configTable;
    private JScrollPane configScrollPane;
    private JButton saveBotton;


    public ConfigTab(JTable configTable, JScrollPane configScrollPane, JButton saveBotton) {
        this.configTable = configTable;
        this.configScrollPane = configScrollPane;
        this.saveBotton = saveBotton;
        initConfigTable();
        configTabActionListener();
    }

    /**
     * 初始化Config选项卡
     *
     * @throws IOException
     */
    private void initConfigTable() {
        // 定义列名
        final Object[] columnNames = {"#", "Key", "Value", "Comment"};
        // 设置初始数据为空，并重写 isCellEditable 方法
        configModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // 只允许编辑第3列(Value)和第4列(Comment)
                return column == 2 || column == 3;
            }
        };
        configTable.setModel(configModel);

        // 关闭自动调整列宽锁
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
        if (System.getProperty("os.name").contains("Windows")) {
            CONFIG_FILE_PATH = System.getProperty("user.home") + "\\" + "ToolBox.json";
        } else {
            CONFIG_FILE_PATH = System.getProperty("user.home") + "/.config/ToolBox.json";
        }

        try {
            File configFile = new File(CONFIG_FILE_PATH);
            // 如果不存在配置文件，创建并初始化配置文件
            if (!configFile.exists() || configFile.length() == 0) {
                File parentDir = configFile.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                if (!configFile.exists()) {
                    try {
                        configFile.createNewFile();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
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
                // 按照表格列顺序添加数据：0:Id, 1:Key, 2:Value, 3:Comment
                configModel.addRow(new Object[]{
                        obj.getString("Id"),
                        obj.getString("Key"),
                        obj.getString("Value"),
                        obj.getString("Comment")
                });
            }
        } catch (IOException | JSONException e) {
            api.logging().logToOutput("Error loading config: " + e.getMessage());
        }
    }

    private void configTabActionListener() {
        // 配置文件保存点击事件
        saveBotton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConfigToFile();
            }
        });
    }

    /**
     * 保存配置文件
     */
    public void saveConfigToFile() {
        JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < configModel.getRowCount(); i++) {
            JSONObject obj = new JSONObject();
            // 按照 id、key、Value、Comment 的顺序保存
            obj.put("Id", configModel.getValueAt(i, 0));      // 第0列：Id
            obj.put("Key", configModel.getValueAt(i, 1));     // 第1列：Key
            obj.put("Value", configModel.getValueAt(i, 2));   // 第2列：Value
            obj.put("Comment", configModel.getValueAt(i, 3)); // 第3列：Comment
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
        int firstColumnWidth = 50;   // Id列
        int secondColumnWidth = 200; // Key列
        configTable.getColumnModel().getColumn(0).setPreferredWidth(firstColumnWidth);
        configTable.getColumnModel().getColumn(1).setPreferredWidth(secondColumnWidth);
        int remainingWidth = totalWidth - firstColumnWidth - secondColumnWidth;
        int otherColumnWidth = remainingWidth / 2; // 剩余两列（Value和Comment）平分宽度

        for (int i = 2; i < configTable.getColumnCount(); i++) {
            configTable.getColumnModel().getColumn(i).setPreferredWidth(otherColumnWidth);
        }
    }

    /**
     * 获取 UpdateCertificate 需要更新的凭证字段列表
     */
    public static List<String> getUpdateCertificateHeaders() {
        List<String> headers = new ArrayList<>();

        try {
            for (int i = 0; i < configModel.getRowCount(); i++) {
                String key = (String) configModel.getValueAt(i, 1); // 第1列是Key
                if ("Update Certificate".equals(key)) {
                    String value = (String) configModel.getValueAt(i, 2); // 第2列是Value
                    if (value != null && !value.trim().isEmpty()) {
                        // 按逗号分割并去除空格
                        String[] headerArray = value.split(",");
                        for (String header : headerArray) {
                            headers.add(header.trim());
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // 如果获取配置失败，使用默认值
            headers.add("Cookie");
            headers.add("Authorization");
            // 记录错误日志
            if (ToolBox.api != null) {
                ToolBox.api.logging().logToError("Error getting UpdateCertificateHeaders: " + e.getMessage());
            }
        }

        // 如果没有配置，使用默认值
        if (headers.isEmpty()) {
            headers.add("Cookie");
            headers.add("Authorization");
            headers.add("token");
        }

        return headers;
    }
}
