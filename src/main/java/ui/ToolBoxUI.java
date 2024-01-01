package ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import EditorPanel.AutorizeTableModel;
import extension.ToolBox;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static burp.api.montoya.ui.editor.EditorOptions.READ_ONLY;

public class ToolBoxUI {
    private MontoyaApi api = ToolBox.api;
    private AutorizeTableModel tableModel = ToolBox.tableModel;
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
    private JPanel autorizePanel;
    private JPanel whiteListPanel;
    private JPanel authorityPanel;
    private JCheckBox startupCheckBox;
    private JButton clearListButton;
    private JTextField whiteListTextField;
    private JTextArea authBypassTextArea;
    private JButton startupWhiteListButton;
    private JTextArea unauthTextArea;
    private JPanel domainPanel;
    private JSplitPane authhorizontalSplitPane;
    private JLabel whiteListLabel;
    private JPanel authorityConfigPanel;
    private JSplitPane authVerticalSplitPane;
    private JPanel authorityVulnPanel;
    private DNSLog dnsLog;
    private ConfigTab configTab;
    private Autorize autorize;

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

    public void InitTab() {
        dnsLog = new DNSLog(domainTextField, dataTable);
        configTab = new ConfigTab(configTable, configScrollPane);

        // 创建日志视图组件
        Component loggerComponent = constructLoggerTab(tableModel);

        // 初始化 authorityVulnPanel 面板布局，避免空指针异常
        authorityVulnPanel.setLayout(new BorderLayout());
        authorityVulnPanel.add(loggerComponent, BorderLayout.CENTER);

        // 将控件添加到分割线各测
        authVerticalSplitPane.setLeftComponent(authorityVulnPanel);
        authVerticalSplitPane.setRightComponent(authorityConfigPanel);
        // 设置分割线两端的分配比例
        authVerticalSplitPane.setResizeWeight(0.8);

        authhorizontalSplitPane.setTopComponent(whiteListPanel);
        authhorizontalSplitPane.setBottomComponent(authorityPanel);
        // 透明背景色，隐藏填充空间(dividerSize)
        authhorizontalSplitPane.setOpaque(false);

    }

    private Component constructLoggerTab(AutorizeTableModel tableModel) {
        // main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // tabs with request/response viewers
        JTabbedPane tabs = new JTabbedPane();

        UserInterface userInterface = api.userInterface();

        HttpRequestEditor rawRequest = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor lowAuthRequest = userInterface.createHttpResponseEditor(READ_ONLY);
        HttpResponseEditor unauthRequest = userInterface.createHttpResponseEditor(READ_ONLY);

        tabs.addTab("原始请求包", rawRequest.uiComponent());
        tabs.addTab("低权限数据包", lowAuthRequest.uiComponent());
        tabs.addTab("未授权数据包", unauthRequest.uiComponent());

        splitPane.setRightComponent(tabs);

        // table of log entries
        JTable table = new JTable(tableModel) {
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                // show the log entry for the selected row
                HttpResponseReceived responseReceived = tableModel.get(rowIndex);
                rawRequest.setRequest(responseReceived.initiatingRequest());
                lowAuthRequest.setResponse(responseReceived);
                unauthRequest.setResponse(responseReceived);

                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };

        // 关闭自动调整列宽
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setMinWidth(30);
        columnModel.getColumn(0).setMaxWidth(80);
        columnModel.getColumn(1).setMinWidth(30);
        columnModel.getColumn(1).setMaxWidth(80);

        // 创建文本居中的渲染器
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        columnModel.getColumn(0).setCellRenderer(centerRenderer);
        columnModel.getColumn(1).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);

        splitPane.setLeftComponent(scrollPane);

        return splitPane;
    }
}
