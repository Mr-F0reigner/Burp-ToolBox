package ui;

import burp.api.montoya.MontoyaApi;
import extension.ToolBox;
import org.json.JSONArray;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
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

    public ToolBoxUI() {
        // 加载表单样式
        setDnsLogStyle();
        setConfigTableStyle();
        saveOriginalData();
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
                    api.logging().logToError(ex.getMessage());
                }
            }
        });


        saveBotton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < configModel.getRowCount(); i++) {
                    String currentData = configModel.getValueAt(i, 2).toString();
                    String originalDataValue = originalConfigData.get(i);

                    // 对比当前数据与原始数据
                    if (!currentData.equals(originalDataValue)) {
                        // 如果不同，根据需要进行处理
                        // 例如，更新 originalData 和执行其他操作
                        originalConfigData.set(i, currentData);
                        // 这里可以添加其他需要的操作，比如更新数据库或执行其他任务
                    }
                }
            }
        });
    }
    private void saveOriginalData() {
        originalConfigData.clear();
        for (int i = 0; i < configModel.getRowCount(); i++) {
            originalConfigData.add(configModel.getValueAt(i, 2).toString());
        }
    }
    private void setDnsLogStyle() {
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

    private void setConfigTableStyle() {
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
        configModel.addRow(new Object[]{"1", "SQL Map", "C:\\Users\\Xi_xi\\AppData\\Local\\Programs\\Python\\Python37\\python.exe D:\\PT_Tools\\SQLMap\\sqlmap.py -r C:\\Users\\Xi_xi\\Desktop\\temp.txt --level 1", "Automatic execution of SQLMAP"});
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
