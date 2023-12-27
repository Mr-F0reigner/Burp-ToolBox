package ui;

import burp.api.montoya.MontoyaApi;
import extension.ToolBox;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToolBoxUI {
    public MontoyaApi api = ToolBox.api;
    public JPanel rootPanel;
    private JButton getSubDomain;
    private JButton refreshRecord;
    private JPanel buttonPanel;
    private JLabel domainLabel;
    private JTable dataTable;
    private JScrollPane dataPanel;

    public ToolBoxUI() {
        // 定义列名
        final Object[] columnNames = {"DNS Query Record", "IP Address", "Created Time" };
        // 使用 DefaultTableModel，初始时没有数据
        DefaultTableModel dataModel = new DefaultTableModel(columnNames, 0);
        dataTable.setModel(dataModel);

        getSubDomain.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((DefaultTableModel) dataTable.getModel()).addRow(new Object[]{"www.baidu.com", "192.168.1.1", "2018-01-01"});
            }
        });
    }
}
