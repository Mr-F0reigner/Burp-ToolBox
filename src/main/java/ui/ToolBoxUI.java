package ui;

import burp.api.montoya.MontoyaApi;
import extension.ToolBox;
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

public class ToolBoxUI {
    public MontoyaApi api = ToolBox.api;
    public JPanel rootPanel;
    private JButton getSubDomain;
    private JButton refreshRecord;
    private JPanel buttonPanel;
    private JTable dataTable;
    private JScrollPane dataScrollPane;
    private JTabbedPane rootTabbedPanel;
    private JPanel dnslogPanel;
    private JTextField domainTextField;
    private JPanel configPanel;
    private JScrollPane configScrollPane;
    private JTable configTable;
    private JButton saveBotton;
    private static DNSLog dnsLog = new DNSLog();
    private DefaultTableModel dnsLogModel = new DefaultTableModel();
    private DefaultTableModel configModel = new DefaultTableModel();
    public static ArrayList<String> originalConfigData = new ArrayList<>();
    // 配置文件路径
    private static final String CONFIG_FILE_PATH = System.getProperty("user.home") + "\\" + "ToolBox.json";

    public ToolBoxUI() {
        try {
            // 加载表单样式
            initDNSLog();
            initConfigTable();

            // 获取域名点击事件
            getSubDomain.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        while (dnsLogModel.getRowCount() > 0) {
                            dnsLogModel.removeRow(0);
                        }
                        domainTextField.setText(dnsLog.getDnslogDomain());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            // 刷新记录点击事件
            refreshRecord.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        JSONArray jsonArray = dnsLog.fetchDnsLogRecords();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONArray record = jsonArray.getJSONArray(i);
                            // 从 record 中获取需要检查的数据
                            String data1 = record.getString(0); // 第一列
                            String data2 = record.getString(1); // 第二列
                            String data3 = record.getString(2); // 第三列
                            // 检查数据是否已经存在于 dnsLogModel 中
                            boolean dataExists = false;
                            for (int j = 0; j < dnsLogModel.getRowCount(); j++) {
                                if (dnsLogModel.getValueAt(j, 0).equals(data1) &&
                                        dnsLogModel.getValueAt(j, 1).equals(data2) &&
                                        dnsLogModel.getValueAt(j, 2).equals(data3)) {
                                    dataExists = true;
                                    break;
                                }
                            }
                            // 如果数据不存在，则添加到 dnsLogModel
                            if (!dataExists) {
                                dnsLogModel.addRow(new Object[]{data1, data2, data3});
                            }
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            saveBotton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    saveConfigToFile();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 保存配置
    private void saveConfigToFile() {
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

    private void initDNSLog() {
        // 设置域名文本框样式
        domainTextField.setBorder(null);    // 无边框
        domainTextField.setOpaque(false);   // 透明背景
        // 定义列名
        final Object[] columnNames = {"DNS Query Record", "IP Address", "Created Time"};
        // 使用 DefaultTableModel，解决JTable不显示问题。设置初始为空
        dnsLogModel = new DefaultTableModel(columnNames, 0);
        dataTable.setModel(dnsLogModel);
        // 为每一列设置自定义渲染器，使数据居中显示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            dataTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private void initConfigTable() throws IOException {
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

        // 初始调整列宽
        adjustConfigColumnWidths();

        // 为每一列设置自定义渲染器，使数据居中显示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < configTable.getColumnCount(); i++) {
            configTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        loadConfigFromFile();
    }

    // 加载配置
    private void loadConfigFromFile() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            // 如果不存在配置文件，创建并初始化配置文件
            if (!configFile.exists() || configFile.length() == 0) {
                configFile.createNewFile();
                String initConfig = """
                        [{"Comment":"Automatic execution of SQLMAP","Value":"C:\\\\Program Files\\\\Python311\\\\python.exe D:\\\\Tools\\\\SQLMap\\\\sqlmap.py -r C:\\\\Users\\\\Xi_xi\\\\Desktop\\\\temp.txt --level 1","Id":"1","Key":"SQL Map"}]
                        """;
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(configFile));
                bufferedWriter.write(initConfig);
                bufferedWriter.flush();
                bufferedWriter.close();
            }

            // 读取配置文件并加载数据到burp
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE_PATH)));
            JSONArray jsonArray = new JSONArray(content);

            configModel.setRowCount(0); // 清空现有的配置数据
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                configModel.addRow(new Object[]{obj.getString("Id"), obj.getString("Key"), obj.getString("Value"), obj.getString("Comment")});
            }
        } catch (IOException | JSONException e) {
            api.logging().logToOutput("Error loading config: " + e.getMessage());
        }
    }

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
