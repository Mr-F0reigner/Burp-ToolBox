package ui;

import Extension.Autorize.AutorizeTableModel;
import Extension.Autorize.LogEntry;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import main.ToolBox;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

import static burp.api.montoya.ui.editor.EditorOptions.READ_ONLY;

public class Autorize {
    private MontoyaApi api = ToolBox.api;
    private JSplitPane authVerticalSplitPane;
    private JSplitPane authhorizontalSplitPane;
    private JPanel authorityVulnPanel;
    private JPanel authorityConfigPanel;
    private JPanel whiteListPanel;
    private JPanel authorityPanel;
    private AutorizeTableModel tableModel = ToolBox.autorizeTableModel;
    private JButton startupButton;
    private JTextArea authBypassTextArea;
    private JTextArea unauthTextArea;
    private JButton clearListButton;
    private JButton startupWhiteListButton;
    private JTextField whiteListTextField;
    public static java.util.List<String> whiteListDomain = new ArrayList<>();
    public static java.util.List<String> unauthHeader = new ArrayList<>();
    public static List<String> authBypass = new ArrayList<>();
    public static Boolean whiteListSwitch = false;
    public static Boolean autorizeStartupSwitch = false;


    public Autorize(JPanel authorityVulnPanel, JSplitPane authVerticalSplitPane, JSplitPane authhorizontalSplitPane, JPanel authorityConfigPanel, JPanel whiteListPanel, JPanel authorityPanel, JButton startupButton, JTextArea authBypassTextArea, JTextArea unauthTextArea, JButton clearListButton, JButton startupWhiteListButton, JTextField whiteListTextField) {
        this.authVerticalSplitPane = authVerticalSplitPane;
        this.authhorizontalSplitPane = authhorizontalSplitPane;
        this.authorityVulnPanel = authorityVulnPanel;
        this.authorityConfigPanel = authorityConfigPanel;
        this.whiteListPanel = whiteListPanel;
        this.authorityPanel = authorityPanel;
        this.startupButton = startupButton;
        this.authBypassTextArea = authBypassTextArea;
        this.unauthTextArea = unauthTextArea;
        this.clearListButton = clearListButton;
        this.startupWhiteListButton = startupWhiteListButton;
        this.whiteListTextField = whiteListTextField;
        initAutorize();
        autorizeActionListener();
    }

    private void initAutorize() {
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

    private void autorizeActionListener() {
        // 启动按钮
        startupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autorizeStartupSwitch = !autorizeStartupSwitch;
                if (autorizeStartupSwitch) {
                    authBypassTextArea.setEnabled(false);
                    startupButton.setText("Autorize is On");
                    startupButton.setBackground(Color.decode("#26649D"));
                    startupButton.setForeground(Color.white);
                    // 从 authBypassTextArea 获取数据并以换行符分割
                    String[] authBypassHeaderList = authBypassTextArea.getText().split("\n");
                    // 对分割后的每一行进行处理
                    for (String line : authBypassHeaderList) {
                        authBypass.add(line);
                    }

                    unauthTextArea.setEnabled(false);
                    // 从 unauthTextArea 获取数据并以换行符分割
                    String[] unauthHeaderList = unauthTextArea.getText().split("\n");
                    // 对分割后的每一行进行处理
                    for (String line : unauthHeaderList) {
                        unauthHeader.add(line);
                    }
                } else {
                    startupButton.setText("Autorize is On");
                    startupButton.setBackground(null);
                    startupButton.setForeground(null);
                    authBypassTextArea.setEnabled(true);
                    unauthTextArea.setEnabled(true);
                }
            }
        });

        // 清空列表
        clearListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableModel.clearLog();
                AutorizeTableModel.recordedUrlMD5.clear();
            }
        });

        // 启动白名单
        startupWhiteListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                whiteListSwitch = !whiteListSwitch;
                if (whiteListSwitch) {
                    whiteListTextField.setEnabled(false);
                    startupWhiteListButton.setText("白名单模式开启");
                    startupWhiteListButton.setBackground(Color.decode("#26649D"));
                    startupWhiteListButton.setForeground(Color.white);

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
                    }
                } else {
                    startupWhiteListButton.setText("启动白名单");
                    startupWhiteListButton.setBackground(null);
                    startupWhiteListButton.setForeground(null);
                    whiteListTextField.setEnabled(true);
                }

            }
        });

        // 白名单域名文本框焦点事件
        whiteListTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (whiteListTextField.getText().equals("如果需要多个域名加白请用逗号隔开")) {
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


    }

    /**
     * 创建面板（日志条目列表，请求/响应编辑器）
     */
    private Component constructLoggerTab() {
        // 主分割窗格
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 带有请求/响应编辑器的选项卡
        JTabbedPane tabs = new JTabbedPane();

        UserInterface userInterface = api.userInterface();

        // 创建原始请求/响应面板
        HttpRequestEditor originalRequest = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor originalResponse = userInterface.createHttpResponseEditor(READ_ONLY);
        JSplitPane originalRequestResponse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, originalRequest.uiComponent(), originalResponse.uiComponent());
        originalRequestResponse.setResizeWeight(0.5); // 初始时分配等同的空间给请求和响应编辑器

        // 创建低权限请求/响应面板
        HttpRequestEditor lowAuthRequest = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor lowAuthResponse = userInterface.createHttpResponseEditor(READ_ONLY);
        JSplitPane lowAuthRequestResponse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lowAuthRequest.uiComponent(), lowAuthResponse.uiComponent());
        lowAuthRequestResponse.setResizeWeight(0.5); // 初始时分配等同的空间给请求和响应编辑器


        // 创建越权请求/响应面板
        HttpRequestEditor unauthRequest = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor unauthResponse = userInterface.createHttpResponseEditor(READ_ONLY);
        JSplitPane unauthRequestResponse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, unauthRequest.uiComponent(), unauthResponse.uiComponent());
        unauthRequestResponse.setResizeWeight(0.5); // 初始时分配等同的空间给请求和响应编辑器


        tabs.addTab("原始请求包", originalRequestResponse);
        tabs.addTab("低权限数据包", lowAuthRequestResponse);
        tabs.addTab("未授权数据包", unauthRequestResponse);

        splitPane.setBottomComponent(tabs);

        // 日志条目列表
        JTable table = new JTable(tableModel) {
            /**
             * 点击切换查看日志列表时在编辑器中展示对应的请求/响应数据
             */
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                LogEntry logEntry = tableModel.get(rowIndex);
                // 将请求和响应包设置到编辑器面板中
                originalRequest.setRequest(logEntry.originalRequest);
                originalResponse.setResponse(logEntry.originalResponse);

                lowAuthRequest.setRequest(logEntry.authBypassRequest);
                lowAuthResponse.setResponse(logEntry.authBypassResponse);

                unauthRequest.setRequest(logEntry.unauthRequest);
                unauthResponse.setResponse(logEntry.unauthResponse);

                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };

        // 设置列宽
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setMinWidth(30);
        columnModel.getColumn(0).setMaxWidth(80);
        columnModel.getColumn(1).setMinWidth(35);
        columnModel.getColumn(1).setMaxWidth(80);
        // 第三列设置为左对齐，不设置固定宽度
        columnModel.getColumn(3).setMinWidth(35);
        columnModel.getColumn(3).setMaxWidth(180);
        columnModel.getColumn(3).setPreferredWidth(80);
        columnModel.getColumn(4).setMinWidth(30);
        columnModel.getColumn(4).setMaxWidth(180);
        columnModel.getColumn(4).setPreferredWidth(180);
        columnModel.getColumn(5).setMinWidth(30);
        columnModel.getColumn(5).setMaxWidth(180);
        columnModel.getColumn(5).setPreferredWidth(180);

        // 设置自定义渲染器,将表格样式应用到每一列
        ColorChangingRenderer colorRenderer = new ColorChangingRenderer(tableModel);
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setCellRenderer(colorRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        splitPane.setTopComponent(scrollPane);

        return splitPane;
    }

    /**
     * 自定义渲染器
     */
    class ColorChangingRenderer extends DefaultTableCellRenderer {
        private AutorizeTableModel model;

        public ColorChangingRenderer(AutorizeTableModel model) {
            this.model = model;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // 设置对齐方式
            if (column == 2) { // 第三列（index = 2）左对齐
                setHorizontalAlignment(JLabel.LEFT);
            } else { // 其他列居中对齐
                setHorizontalAlignment(JLabel.CENTER);
            }

            // 设置背景颜色
            LogEntry logEntry = model.get(row);
            Color backgroundColor;
            if (logEntry.authBypassResponseLen == logEntry.originalResponseLen && logEntry.unauthResponseLen != logEntry.originalResponseLen) {
                c.setForeground(Color.white); // 白色字体
                backgroundColor = Color.decode("#FF6464"); // 满足越权+未授权，显示红色
            }  else {
                c.setForeground(table.getForeground()); // 默认文字色
                backgroundColor = table.getBackground(); // 默认背景色
            }

//            else if (logEntry.authBypassResponseLen == logEntry.originalResponseLen) {
//                backgroundColor = Color.decode("#64FF64"); // 满足越权，显示绿色
//            } else if (logEntry.unauthResponseLen == logEntry.originalResponseLen) {
//                c.setForeground(Color.white); // 白色字体
//                backgroundColor = Color.decode("#6464FF"); // 满足未授权，显示蓝色
//            }

            // 被选中时的背景色
            if (isSelected) {
                if (backgroundColor == table.getBackground()) {
                    backgroundColor = Color.decode("#CADAF0");
                } else {
                    backgroundColor = backgroundColor.darker();
                }
            }
            c.setBackground(backgroundColor);
            return c;
        }
    }
}
