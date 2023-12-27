package ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import extension.DNSLog;
import extension.ToolBox;
import org.json.JSONArray;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ToolBoxUI {
    public MontoyaApi api = ToolBox.api;
    public JPanel rootPanel;
    private JButton getSubDomain;
    private JButton refreshRecord;
    private JPanel buttonPanel;
    private JLabel domainLabel;
    private JTable dataTable;
    private JScrollPane dataPanel;
    private JTabbedPane tabbedPane1;
    private JPanel dnslogPanel;
    private static DNSLog dnsLog;

    public ToolBoxUI() {
        // 定义列名
        final Object[] columnNames = {"DNS Query Record", "IP Address", "Created Time" };
        // 使用 DefaultTableModel，初始时没有数据
        DefaultTableModel dataModel = new DefaultTableModel(columnNames, 0);
        dataTable.setModel(dataModel);

        getSubDomain.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dnsLog = new DNSLog();
                try {
                    domainLabel.setText(dnsLog.getDnslogDomain());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        refreshRecord.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataModel.addRow(new Object[]{"mwa15k.dnslog.cn","219.128.128.82","2023-12-27 23:38:32"});
                try {
                    JSONArray jsonArray = dnsLog.fetchDnsLogRecords();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONArray record = jsonArray.getJSONArray(i);
                        // 直接从 JSONArray 中获取数据并添加到表格模型
                        dataModel.addRow(new Object[]{record.getString(0), record.getString(1), record.getString(2)});
                        api.logging().logToOutput(record.getString(0));
                        api.logging().logToOutput(record.getString(1));
                        api.logging().logToOutput(record.getString(2));
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
