package ui;

import Extension.Autorize.AutorizeHttpHandler;
import Extension.Autorize.AutorizeTableModel;
import Extension.Autorize.LogEntry;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import main.ToolBox;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList; // [新增] 引入线程安全的列表

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

    // [优化点 2] 使用 CopyOnWriteArrayList 替换 ArrayList
    // 作用：解决线程安全问题，防止后台扫描读取时因 UI 修改导致 ConcurrentModificationException 异常
    public static java.util.List<String> whiteListDomain = new CopyOnWriteArrayList<>();
    public static java.util.List<String> unauthHeader = new CopyOnWriteArrayList<>();
    public static List<String> authBypass = new CopyOnWriteArrayList<>();

    public static Boolean whiteListSwitch = false;
    public static Boolean autorizeStartupSwitch = false;

    // [定义] 配置面板的最小宽度限制（也是初始默认宽度）
    private static final int MIN_CONFIG_PANEL_WIDTH = 350;

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

        // 初始化 authorityVulnPanel 面板布局
        authorityVulnPanel.setLayout(new BorderLayout());
        authorityVulnPanel.add(loggerComponent, BorderLayout.CENTER);

        // [关键逻辑 1] 初始状态下，设置强制最小宽度限制
        authorityConfigPanel.setMinimumSize(new Dimension(MIN_CONFIG_PANEL_WIDTH, 0));

        // 将控件添加到分割线各测
        authVerticalSplitPane.setLeftComponent(authorityVulnPanel);
        authVerticalSplitPane.setRightComponent(authorityConfigPanel);

        // [关键逻辑 2] 设置 ResizeWeight 为 1.0
        authVerticalSplitPane.setResizeWeight(1.0);

        // [关键逻辑 3] 初始化分割线位置
        SwingUtilities.invokeLater(() -> {
            int totalWidth = authVerticalSplitPane.getWidth();
            int dividerSize = authVerticalSplitPane.getDividerSize();
            if (totalWidth > 0) {
                authVerticalSplitPane.setDividerLocation(totalWidth - dividerSize - MIN_CONFIG_PANEL_WIDTH);
            }
        });

        // ----------------- [交互逻辑] 分割线双击事件 -----------------
        if (authVerticalSplitPane.getUI() instanceof BasicSplitPaneUI) {
            BasicSplitPaneUI ui = (BasicSplitPaneUI) authVerticalSplitPane.getUI();
            Container divider = ui.getDivider();

            if (divider != null) {
                divider.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // 检测双击事件
                        if (e.getClickCount() == 2) {
                            toggleConfigPanel();
                        }
                    }
                });
            }
        }
        // ----------------- 结束 -----------------

        authhorizontalSplitPane.setTopComponent(whiteListPanel);
        authhorizontalSplitPane.setBottomComponent(authorityPanel);
        authhorizontalSplitPane.setOpaque(false);
    }

    /**
     * [重写] 切换配置面板逻辑 (动态最小宽度版)
     */
    private void toggleConfigPanel() {
        // 1. 获取基础数据
        int totalWidth = authVerticalSplitPane.getWidth();
        int dividerSize = authVerticalSplitPane.getDividerSize();
        int currentLoc = authVerticalSplitPane.getDividerLocation();

        // 2. 计算右侧面板当前的实际宽度
        int currentRightWidth = totalWidth - currentLoc - dividerSize;

        // 3. 判断是否处于隐藏状态
        boolean isHidden = currentRightWidth < 50;

        if (isHidden) {
            // === 恢复操作 ===
            // [核心] 恢复时，必须先把最小宽度限制加回来！
            authorityConfigPanel.setMinimumSize(new Dimension(MIN_CONFIG_PANEL_WIDTH, 0));

            // 计算目标位置：恢复到默认的最小宽度
            int targetLoc = totalWidth - dividerSize - MIN_CONFIG_PANEL_WIDTH;

            // 安全检查
            if (targetLoc < 0) {
                targetLoc = totalWidth / 2;
            }

            authVerticalSplitPane.setDividerLocation(targetLoc);

        } else {
            // === 隐藏操作 ===
            // [核心] 隐藏前，必须临时取消最小宽度限制！
            authorityConfigPanel.setMinimumSize(new Dimension(0, 0));

            // 将分割线移动到最右侧 (100%)，实现隐藏
            authVerticalSplitPane.setDividerLocation(1.0);
        }
    }

    private void autorizeActionListener() {
        // 启动按钮
        startupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autorizeStartupSwitch = !autorizeStartupSwitch;
                if (autorizeStartupSwitch) {
                    startupButton.setText("Autorize is On");
                    startupButton.setBackground(Color.decode("#26649D"));
                    startupButton.setForeground(Color.white);

                    // [优化点 1 & 3] 清空旧列表，防止重复；去除首尾空格
                    authBypass.clear();

                    String[] authBypassHeaderList = authBypassTextArea.getText().split("\n");
                    for (String line : authBypassHeaderList) {
                        String trimmedLine = line.trim();
                        if (!trimmedLine.isEmpty()) {
                            authBypass.add(trimmedLine);
                        }
                    }

                    authBypassTextArea.setEditable(false);
                    authBypassTextArea.setBackground(Color.decode("#F5F5F5")); // 浅灰色背景
                    authBypassTextArea.setForeground(Color.decode("#888888")); // 灰色文字

                    unauthTextArea.setEnabled(false);

                    // [优化点 1 & 3] 同理处理未授权Header列表
                    unauthHeader.clear();

                    String[] unauthHeaderList = unauthTextArea.getText().split("\n");
                    for (String line : unauthHeaderList) {
                        String trimmedLine = line.trim();
                        if (!trimmedLine.isEmpty()) {
                            unauthHeader.add(trimmedLine);
                        }
                    }
                } else {
                    startupButton.setText("Autorize is Off");
                    startupButton.setBackground(null);
                    startupButton.setForeground(null);

                    authBypassTextArea.setEditable(true);
                    authBypassTextArea.setBackground(Color.WHITE); // 恢复白色背景
                    authBypassTextArea.setForeground(Color.BLACK); // 恢复黑色文字

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
                AutorizeHttpHandler.id.set(0);
            }
        });

        // 启动白名单
        startupWhiteListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                whiteListSwitch = !whiteListSwitch;
                if (whiteListSwitch) {
                    whiteListTextField.setEnabled(false);
                    startupWhiteListButton.setText("关闭白名单");
                    startupWhiteListButton.setBackground(Color.decode("#26649D"));
                    startupWhiteListButton.setForeground(Color.white);

                    // [优化点 1 & 3] 先清空，再添加，并处理空格
                    whiteListDomain.clear();

                    String whiteListText = whiteListTextField.getText();

                    // 检查是否为提示文本或空文本
                    if (!whiteListText.equals("如果需要多个域名加白请用逗号隔开") && !whiteListText.trim().isEmpty()) {
                        String[] whiteListDomainList = whiteListText.split(",");
                        for (String line : whiteListDomainList) {
                            String trimmedLine = line.trim();
                            if (!trimmedLine.isEmpty()) {
                                whiteListDomain.add(trimmedLine);
                            }
                        }
                    }
                } else {
                    startupWhiteListButton.setText("开启白名单");
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
                // [优化] 如果内容是空的或者只有空格，也恢复提示语
                if (whiteListTextField.getText().trim().equals("")) {
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

                // 根据点击的列索引自动切换到对应的选项卡
                switch (columnIndex) {
                    case 3: // 第4列 - 原始请求
                        originalRequest.setRequest(logEntry.originalRequest);
                        originalResponse.setResponse(logEntry.originalResponse);
                        tabs.setSelectedIndex(0); // 切换到原始请求包选项卡
                        break;
                    case 4: // 第5列 - 低权限请求
                        lowAuthRequest.setRequest(logEntry.authBypassRequest);
                        lowAuthResponse.setResponse(logEntry.authBypassResponse);
                        tabs.setSelectedIndex(1); // 切换到低权限数据包选项卡
                        break;
                    case 5: // 第6列 - 未授权请求
                        unauthRequest.setRequest(logEntry.unauthRequest);
                        unauthResponse.setResponse(logEntry.unauthResponse);
                        tabs.setSelectedIndex(2); // 切换到未授权数据包选项卡
                        break;
                    default:
                        // 点击其他列时，默认显示所有请求数据，但保持当前选项卡
                        originalRequest.setRequest(logEntry.originalRequest);
                        originalResponse.setResponse(logEntry.originalResponse);
                        lowAuthRequest.setRequest(logEntry.authBypassRequest);
                        lowAuthResponse.setResponse(logEntry.authBypassResponse);
                        unauthRequest.setRequest(logEntry.unauthRequest);
                        unauthResponse.setResponse(logEntry.unauthResponse);
                        break;
                }

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
     * 自定义渲染器,设置单元格以及指定行高亮样式
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
            Color backgroundColor = table.getBackground();

            boolean isVerticalBypass = logEntry.authBypassResponseLen == logEntry.originalResponseLen;
            boolean isUnauthBypass = logEntry.unauthResponseLen == logEntry.originalResponseLen;

            if (isVerticalBypass && !isUnauthBypass) {
                // 仅存在垂直越权 - 红色标记
                c.setForeground(Color.white);
                backgroundColor = Color.decode("#FF6464");
            } else if (isVerticalBypass && isUnauthBypass) {
                // 同时存在越权和未授权漏洞 - 橙色标记
                c.setForeground(Color.white);
                backgroundColor = Color.decode("#FF8C00"); // 橙色
            } else {
                c.setForeground(table.getForeground());
                backgroundColor = table.getBackground();
            }

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