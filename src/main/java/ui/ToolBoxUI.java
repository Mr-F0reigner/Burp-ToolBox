package ui;

import burp.api.montoya.MontoyaApi;
import extension.ToolBox;

import javax.swing.*;
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
    private JTable configTable;
    private JButton saveBotton;
    private DefaultTableModel dnsLogModel = new DefaultTableModel();
    public static DefaultTableModel configModel = new DefaultTableModel();
    private DNSLog dnsLog;
    private ConfigTab configTab ;

    public ToolBoxUI() {
        // 选项卡初始化
        InitTab();

        // 获取域名点击事件
        getSubDomain.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dnsLog.getDnslogDomain();
                } catch (IOException ex) {
                    api.logging().logToOutput(ex.getMessage());
                }
            }
        });
        // 刷新记录点击事件
        refreshRecord.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dnsLog.refreshRecordAction();
            }
        });
        // 配置文件保存点击事件
        saveBotton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                configTab.saveConfigToFile();
            }
        });
    }

    public void InitTab(){
        dnsLog = new DNSLog(domainTextField, dnsLogModel, dataTable);
        configTab = new ConfigTab(configModel, configTable, configScrollPane);
    }

}
