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
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public static List<String> whiteListDomain = new ArrayList<>();
    public static List<String> unauthHeader = new ArrayList<>();
    public static List<String> authBypass = new ArrayList<>();

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

        startupCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 当复选框被选中时
                if (startupCheckBox.isSelected()) {
                    whiteListTextField.setEnabled(false);
                    // 从 whiteListTextField 获取数据
                    String whiteListText = whiteListTextField.getText();
                    String[] whiteListDomainList;

                    // 检查是否为提示文本
                    if (whiteListText.equals("如果需要多个域名加白请用逗号隔开")) {
                        whiteListDomainList = new String[0]; // 置为空数组
                    } else {
                        // 以逗号分割
                        whiteListDomainList = whiteListText.split(",");
                    }
                    // 对分割后的每一行进行处理
                    for (String line : whiteListDomainList) {
                        whiteListDomain.add(line);
                        // 处理每行数据的逻辑（根据需要实现）
                        api.logging().logToOutput(line);
                    }

                    authBypassTextArea.setEnabled(false);
                    // 从 authBypassTextArea 获取数据并以换行符分割
                    String[] authBypassHeaderList = authBypassTextArea.getText().split("\n");
                    // 对分割后的每一行进行处理
                    for (String line : authBypassHeaderList) {
                        authBypass.add(line);
                        // 处理每行数据的逻辑（根据需要实现）
                        api.logging().logToOutput(line);
                    }

                    unauthTextArea.setEnabled(false);
                    // 从 unauthTextArea 获取数据并以换行符分割
                    String[] unauthHeaderList = unauthTextArea.getText().split("\n");
                    // 对分割后的每一行进行处理
                    for (String line : unauthHeaderList) {
                        unauthHeader.add(line);
                        // 处理每行数据的逻辑（根据需要实现）
                        api.logging().logToOutput(line);
                    }
                } else {
                    whiteListTextField.setEnabled(true);
                    authBypassTextArea.setEnabled(true);
                    unauthTextArea.setEnabled(true);
                }
            }
        });

        // 白名单域名文本框焦点事件
        whiteListTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (whiteListTextField.getText().equals("如果需要多个域名加白请用逗号隔开")){
                    whiteListTextField.setText("");
                    whiteListTextField.setForeground(Color.decode("#2B2D30"));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (whiteListTextField.getText().equals("")) {
                    whiteListTextField.setText("如果需要多个域名加白请用逗号隔开");
                    whiteListTextField.setForeground(Color.decode("#8C8C8C"));
                }
            }
        });
        clearListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableModel.clearLog();
            }
        });
    }

    public void InitTab() {
        dnsLog = new DNSLog(domainTextField, dataTable);
        configTab = new ConfigTab(configTable, configScrollPane);

        // 创建日志视图组件
        Component loggerComponent = constructLoggerTab();

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

    private Component constructLoggerTab() {
        // 主分割窗格
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 带有请求/响应编辑器的选项卡
        JTabbedPane tabs = new JTabbedPane();

        UserInterface userInterface = api.userInterface();

        // 原始请求/响应面板
        HttpRequestEditor originalRequest = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor originalResponse = userInterface.createHttpResponseEditor(READ_ONLY);
        JSplitPane originalRequestResponse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, originalRequest.uiComponent(), originalResponse.uiComponent());
        originalRequestResponse.setResizeWeight(0.5); // 初始时分配等同的空间给请求和响应编辑器

        // 低权限请求/响应面板
        HttpRequestEditor lowAuthRequest = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor lowAuthResponse = userInterface.createHttpResponseEditor(READ_ONLY);
        JSplitPane lowAuthRequestResponse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lowAuthRequest.uiComponent(), lowAuthResponse.uiComponent());
        lowAuthRequestResponse.setResizeWeight(0.5); // 初始时分配等同的空间给请求和响应编辑器


        // 越权请求/响应面板
        HttpRequestEditor unauthRequest = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor unauthResponse = userInterface.createHttpResponseEditor(READ_ONLY);
        JSplitPane unauthRequestResponse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, unauthRequest.uiComponent(), unauthResponse.uiComponent());
        unauthRequestResponse.setResizeWeight(0.5); // 初始时分配等同的空间给请求和响应编辑器


        tabs.addTab("原始请求包", originalRequestResponse);
        tabs.addTab("低权限数据包", lowAuthRequestResponse);
        tabs.addTab("未授权数据包", unauthRequestResponse);

        splitPane.setRightComponent(tabs);

        // 日志条目表
        JTable table = new JTable(tableModel) {
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                // 显示所选行的日志条目
                HttpResponseReceived responseReceived = tableModel.get(rowIndex);
                originalRequest.setRequest(responseReceived.initiatingRequest());
                originalResponse.setResponse(responseReceived);

                lowAuthRequest.setRequest(responseReceived.initiatingRequest());
                lowAuthResponse.setResponse(responseReceived);

                unauthRequest.setRequest(responseReceived.initiatingRequest());
                unauthResponse.setResponse(responseReceived);

                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };

        // 自动调整后继列宽
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setMinWidth(30);
        columnModel.getColumn(0).setMaxWidth(80);
        columnModel.getColumn(1).setMinWidth(35);
        columnModel.getColumn(1).setMaxWidth(80);
        columnModel.getColumn(3).setMinWidth(30);
        columnModel.getColumn(3).setMaxWidth(80);

        // 创建文本居中的渲染器
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        columnModel.getColumn(0).setCellRenderer(centerRenderer);
        columnModel.getColumn(1).setCellRenderer(centerRenderer);
        columnModel.getColumn(3).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);

        splitPane.setTopComponent(scrollPane);

        return splitPane;
    }
}
