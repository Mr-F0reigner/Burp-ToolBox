package ui;

import burp.api.montoya.MontoyaApi;
import extension.ToolBox;
import org.json.JSONArray;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

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
    private static DNSLog dnsLog = new DNSLog();
    private DefaultTableModel dataModel = new DefaultTableModel();

    public ToolBoxUI() {
        // 加载表单样式
        setDataTableStyle();

        // 获取域名点击事件
        getSubDomain.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    while (dataModel.getRowCount() > 0) {
                        dataModel.removeRow(0);
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
                        // 检查数据是否已经存在于 dataModel 中
                        boolean dataExists = false;
                        for (int j = 0; j < dataModel.getRowCount(); j++) {
                            if (dataModel.getValueAt(j, 0).equals(data1) &&
                                    dataModel.getValueAt(j, 1).equals(data2) &&
                                    dataModel.getValueAt(j, 2).equals(data3)) {
                                dataExists = true;
                                break;
                            }
                        }
                        // 如果数据不存在，则添加到 dataModel
                        if (!dataExists) {
                            dataModel.addRow(new Object[]{data1, data2, data3});
                        }
                    }
                } catch (IOException ex) {
                    api.logging().logToError(ex.getMessage());
                }
            }
        });
    }

    private void setDataTableStyle() {
        // 设置域名文本框样式
        domainTextField.setBorder(null);    // 无边框
        domainTextField.setOpaque(false);   // 透明背景
        // 定义列名
        final Object[] columnNames = {"DNS Query Record", "IP Address", "Created Time"};
        // 使用 DefaultTableModel，解决JTable不显示问题。设置初始为空
        dataModel = new DefaultTableModel(columnNames, 0);
        dataTable.setModel(dataModel);
        // 为每一列设置自定义渲染器，使数据居中显示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            dataTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }
}
