package ui;

import Extension.Autorize.AutorizeHttpHandler;
import Extension.Autorize.AutorizeTableModel;
import Extension.Autorize.LogEntry;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import burp.api.montoya.ui.hotkey.HotKey;
import burp.api.montoya.ui.hotkey.HotKeyContext;
import burp.api.montoya.ui.hotkey.HotKeyHandler;
import main.ToolBox;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.regex.Pattern; // 记得导入这个
import java.util.regex.PatternSyntaxException;

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
    private JTextArea URLFilterTextArea;

    // 用于追踪鼠标按下的列索引
    private int pressedHeaderColumn = -1;

    private JTable logTable;

    public static Autorize instance;

    public static Boolean whiteListSwitch = false;
    public static Boolean autorizeStartupSwitch = false;

    public static java.util.List<String> whiteListDomain = new CopyOnWriteArrayList<>();
    public static java.util.List<String> unauthHeader = new CopyOnWriteArrayList<>();
    public static List<String> authBypass = new CopyOnWriteArrayList<>();

    // [新增] 用于存储 URL 过滤正则的列表
    public static java.util.List<String> urlFilterList = new CopyOnWriteArrayList<>();
    // [新增 - 优化] 用于存储预编译好的 Pattern 对象 (用于核心检测)
    public static java.util.List<Pattern> urlFilterPatterns = new CopyOnWriteArrayList<>();

    private static final int MIN_CONFIG_PANEL_WIDTH = 350;

    // 颜色常量
    private static final Color COLOR_RED_ALERT = Color.decode("#FF6464");
    private static final Color COLOR_ORANGE_ALERT = Color.decode("#FF8C00");
    private static final Color COLOR_BLUE_START = Color.decode("#26649D");
    private static final Color COLOR_GRAY_BG = Color.decode("#F5F5F5");
    private static final Color COLOR_GRAY_TEXT = Color.decode("#888888");
    private static final Color COLOR_TEXT_DEFAULT = Color.decode("#2B2D30");
    private static final Color COLOR_TEXT_HINT = Color.decode("#8C8C8C");
    private static final Color COLOR_SELECTION_BG = Color.decode("#CADAF0");
    private static final Color COLOR_OFF_BG = Color.decode("#E0E0E0");

    private static final String WHITELIST_HINT = "如果需要多个域名加白请用逗号隔开";

    public Autorize(JPanel authorityVulnPanel, JSplitPane authVerticalSplitPane, JSplitPane authhorizontalSplitPane, JPanel authorityConfigPanel, JPanel whiteListPanel, JPanel authorityPanel, JButton startupButton, JTextArea authBypassTextArea, JTextArea unauthTextArea, JButton clearListButton, JButton startupWhiteListButton, JTextField whiteListTextField, JTextArea URLFilterTextArea) {
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
        this.URLFilterTextArea = URLFilterTextArea;
        initAutorize();
        autorizeActionListener();
    }

    private void initAutorize() {
        this.instance = this;
        Component loggerComponent = constructLoggerTab();
        authorityVulnPanel.setLayout(new BorderLayout());
        authorityVulnPanel.add(loggerComponent, BorderLayout.CENTER);
        authorityConfigPanel.setMinimumSize(new Dimension(MIN_CONFIG_PANEL_WIDTH, 0));
        authVerticalSplitPane.setLeftComponent(authorityVulnPanel);
        authVerticalSplitPane.setRightComponent(authorityConfigPanel);
        authVerticalSplitPane.setResizeWeight(1.0);

        SwingUtilities.invokeLater(() -> {
            int totalWidth = authVerticalSplitPane.getWidth();
            int dividerSize = authVerticalSplitPane.getDividerSize();
            if (totalWidth > 0) {
                authVerticalSplitPane.setDividerLocation(totalWidth - dividerSize - MIN_CONFIG_PANEL_WIDTH);
            }
        });

        if (authVerticalSplitPane.getUI() instanceof BasicSplitPaneUI) {
            BasicSplitPaneUI ui = (BasicSplitPaneUI) authVerticalSplitPane.getUI();
            Container divider = ui.getDivider();
            if (divider != null) {
                divider.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            toggleConfigPanel();
                        }
                    }
                });
            }
        }
        authhorizontalSplitPane.setTopComponent(whiteListPanel);
        authhorizontalSplitPane.setBottomComponent(authorityPanel);
        authhorizontalSplitPane.setOpaque(false);
    }

    /**
     * [新增] 静态方法：注册快捷键
     * 直接在 Main 入口调用此方法即可
     */
    public static void registerHotkey(MontoyaApi api) {
        // 1. 定义快捷键：Ctrl + Alt + Shift + F11
        HotKey hotKey = HotKey.hotKey("Add to Autorize Filter", "Ctrl+Alt+Shift+F11");

        // 2. 定义处理器
        HotKeyHandler handler = event -> {
            // 获取当前编辑器 (如果快捷键是在编辑器上下文中按下的)
            event.messageEditorRequestResponse().ifPresent(editor -> {
                try {
                    // 获取请求对象
                    if (editor.requestResponse() == null || editor.requestResponse().request() == null) {
                        return;
                    }

                    String fullUrl = editor.requestResponse().request().url();

                    // 调用实例方法执行添加
                    if (Autorize.instance != null) {
                        Autorize.instance.addUrlToFilter(fullUrl);
                        // 可选：在右下角打印个日志或Toast提示
                        api.logging().logToOutput("已添加过滤规则: " + fullUrl);
                    }
                } catch (Exception ex) {
                    api.logging().logToError("快捷键执行失败: " + ex.getMessage());
                }
            });
        };

        // 3. 注册到 HTTP 消息编辑器上下文 (Proxy, Repeater 等编辑器中生效)
        api.userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, hotKey, handler);
    }

    private boolean isConfigPanelHidden() {
        return authorityConfigPanel.getMinimumSize().width == 0;
    }

    private void performStartupAction() {
        autorizeStartupSwitch = !autorizeStartupSwitch;
        if (autorizeStartupSwitch) {
            // === 开启状态 ===
            startupButton.setText("Autorize is On");
            startupButton.setBackground(COLOR_BLUE_START);
            startupButton.setForeground(Color.white);

            // 处理 Auth Bypass
            updateListFromText(authBypass, authBypassTextArea.getText(), "\n");
            authBypassTextArea.setEditable(false);
            authBypassTextArea.setBackground(COLOR_GRAY_BG);
            authBypassTextArea.setForeground(COLOR_GRAY_TEXT);

            // 处理 Unauth Header
            unauthTextArea.setEnabled(false);
            updateListFromText(unauthHeader, unauthTextArea.getText(), "\n");

            // [修改] 处理 URL 过滤 - 即使开启也不锁定，保持动态更新
            // 初始化加载一次
            updateListFromText(urlFilterList, URLFilterTextArea.getText(), "\n");
            reloadFilterPatterns();
            // 不再设置 setEditable(false) 和 灰色背景，保持可编辑状态

        } else {
            // === 关闭状态 ===
            startupButton.setText("Autorize is Off");
            startupButton.setBackground(null);
            startupButton.setForeground(null);

            // 恢复 Auth Bypass
            authBypassTextArea.setEditable(true);
            authBypassTextArea.setBackground(Color.WHITE);
            authBypassTextArea.setForeground(Color.BLACK);

            // 恢复 Unauth Header
            unauthTextArea.setEnabled(true);

            // [修改] URL 过滤 - 这里只需要清空规则库，不需要恢复UI状态（因为一直没锁）
            urlFilterPatterns.clear();
        }
        if (logTable != null) {
            logTable.getTableHeader().repaint();
        }
    }

    public Component getRootComponent() {
        // 这里返回你在 BurpExtension 中注册为 SuiteTab 的那个组件
        // 根据你之前的代码，它是 authVerticalSplitPane
        return this.authVerticalSplitPane;
    }

    private void reloadFilterPatterns() {
        urlFilterPatterns.clear();
        for (String regex : urlFilterList) {
            if (regex == null || regex.trim().isEmpty()) continue;
            try {
                // 预编译正则，忽略大小写 (根据你的需求可选 Case_INSENSITIVE)
                Pattern pattern = Pattern.compile(regex);
                urlFilterPatterns.add(pattern);
            } catch (PatternSyntaxException e) {
                // 如果用户输入了错误的正则，可以在这里打印日志，或者忽略
                System.err.println("Invalid Regex: " + regex);
            }
        }
    }

    private void performClearAction() {
        tableModel.clearLog();
        AutorizeTableModel.recordedUrlMD5.clear();
        AutorizeHttpHandler.id.set(0);
    }

    public void updateAuthBypassContent(String content) {
        SwingUtilities.invokeLater(() -> {
            this.authBypassTextArea.setText(content);
            if (autorizeStartupSwitch) {
                updateListFromText(authBypass, content, "\n");
            }
        });
    }

    public void updateUnauthContent(String content) {
        SwingUtilities.invokeLater(() -> {
            this.unauthTextArea.setText(content);
            if (autorizeStartupSwitch) {
                updateListFromText(unauthHeader, content, "\n");
            }
        });
    }

    private void updateListFromText(List<String> targetList, String text, String separator) {
        targetList.clear();
        if (text == null || text.isEmpty()) return;
        List<String> newItems = Arrays.stream(text.split(separator))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        targetList.addAll(newItems);
    }

    /**
     * [修改后] 核心判断逻辑：使用预编译的 Pattern
     * 性能提升：O(1) 匹配，无重复编译开销
     */
    public static boolean isUrlFiltered(String url) {
        if (urlFilterPatterns == null || urlFilterPatterns.isEmpty()) {
            return false;
        }
        // 遍历预编译好的 Pattern 对象
        for (Pattern pattern : urlFilterPatterns) {
            try {
                // 直接 match，无需重新编译
                if (pattern.matcher(url).matches()) {
                    return true;
                }
            } catch (Exception e) {
                // 防止极端情况下的异常
            }
        }
        return false;
    }

    private void toggleConfigPanel() {
        int totalWidth = authVerticalSplitPane.getWidth();
        int dividerSize = authVerticalSplitPane.getDividerSize();
        int currentLoc = authVerticalSplitPane.getDividerLocation();
        boolean isHidden = (totalWidth - currentLoc - dividerSize) < 50;

        if (isHidden) {
            authorityConfigPanel.setMinimumSize(new Dimension(MIN_CONFIG_PANEL_WIDTH, 0));
            int targetLoc = totalWidth - dividerSize - MIN_CONFIG_PANEL_WIDTH;
            if (targetLoc < 0) targetLoc = totalWidth / 2;
            authVerticalSplitPane.setDividerLocation(targetLoc);
        } else {
            authorityConfigPanel.setMinimumSize(new Dimension(0, 0));
            authVerticalSplitPane.setDividerLocation(1.0);
        }
        if (logTable != null) {
            logTable.getTableHeader().repaint();
        }
    }

    private void autorizeActionListener() {
        startupButton.addActionListener(e -> performStartupAction());
        clearListButton.addActionListener(e -> performClearAction());

        startupWhiteListButton.addActionListener(e -> {
            whiteListSwitch = !whiteListSwitch;
            if (whiteListSwitch) {
                whiteListTextField.setEnabled(false);
                startupWhiteListButton.setText("关闭白名单");
                startupWhiteListButton.setBackground(COLOR_BLUE_START);
                startupWhiteListButton.setForeground(Color.white);
                String text = whiteListTextField.getText();
                if (!WHITELIST_HINT.equals(text)) {
                    updateListFromText(whiteListDomain, text, ",");
                } else {
                    whiteListDomain.clear();
                }
            } else {
                startupWhiteListButton.setText("开启白名单");
                startupWhiteListButton.setBackground(null);
                startupWhiteListButton.setForeground(null);
                whiteListTextField.setEnabled(true);
            }
        });

        whiteListTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (whiteListTextField.getText().equals(WHITELIST_HINT)) {
                    whiteListTextField.setText("");
                    whiteListTextField.setForeground(COLOR_TEXT_DEFAULT);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (whiteListTextField.getText().trim().isEmpty()) {
                    whiteListTextField.setText(WHITELIST_HINT);
                    whiteListTextField.setForeground(COLOR_TEXT_HINT);
                }
            }
        });

        URLFilterTextArea.getDocument().addDocumentListener(new DocumentListener() {
            private void updateFilters() {
                // 只有当插件开启时才实时更新内存中的规则
                if (autorizeStartupSwitch) {
                    updateListFromText(urlFilterList, URLFilterTextArea.getText(), "\n");
                    reloadFilterPatterns();
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) { updateFilters(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateFilters(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateFilters(); }
        });
    }

    public void addUrlToFilter(String fullUrl) {
        // 1. 提取无参数部分
        String urlNoParams = fullUrl.split("\\?")[0];

        // 2. 添加通配符后缀
        String newRegex = urlNoParams + ".*";

        // 3. 在 EDT 线程中安全更新 UI
        javax.swing.SwingUtilities.invokeLater(() -> {
            String currentText = URLFilterTextArea.getText();

            // 简单的去重检查
            if (!currentText.contains(newRegex)) {
                if (!currentText.isEmpty() && !currentText.endsWith("\n")) {
                    URLFilterTextArea.append("\n");
                }
                URLFilterTextArea.append(newRegex);
                // 触发滚动到底部 (可选)
                URLFilterTextArea.setCaretPosition(URLFilterTextArea.getDocument().getLength());
            }
        });
    }

    private Component constructLoggerTab() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JTabbedPane tabs = new JTabbedPane();
        UserInterface userInterface = api.userInterface();

        HttpRequestEditor originalRequest = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor originalResponse = userInterface.createHttpResponseEditor(READ_ONLY);
        JSplitPane originalRequestResponse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, originalRequest.uiComponent(), originalResponse.uiComponent());
        originalRequestResponse.setResizeWeight(0.5);

        HttpRequestEditor lowAuthRequest = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor lowAuthResponse = userInterface.createHttpResponseEditor(READ_ONLY);
        JSplitPane lowAuthRequestResponse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lowAuthRequest.uiComponent(), lowAuthResponse.uiComponent());
        lowAuthRequestResponse.setResizeWeight(0.5);

        HttpRequestEditor unauthRequest = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor unauthResponse = userInterface.createHttpResponseEditor(READ_ONLY);
        JSplitPane unauthRequestResponse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, unauthRequest.uiComponent(), unauthResponse.uiComponent());
        unauthRequestResponse.setResizeWeight(0.5);

        tabs.addTab("原始请求包", originalRequestResponse);
        tabs.addTab("低权限数据包", lowAuthRequestResponse);
        tabs.addTab("未授权数据包", unauthRequestResponse);

        splitPane.setBottomComponent(tabs);

        this.logTable = new JTable(tableModel) {
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                int modelRow = convertRowIndexToModel(rowIndex);
                LogEntry logEntry = tableModel.get(modelRow);

                originalRequest.setRequest(logEntry.originalRequest);
                originalResponse.setResponse(logEntry.originalResponse);
                lowAuthRequest.setRequest(logEntry.authBypassRequest);
                lowAuthResponse.setResponse(logEntry.authBypassResponse);
                unauthRequest.setRequest(logEntry.unauthRequest);
                unauthResponse.setResponse(logEntry.unauthResponse);

                if (columnIndex == 3) tabs.setSelectedIndex(0);
                else if (columnIndex == 4) tabs.setSelectedIndex(1);
                else if (columnIndex == 5) tabs.setSelectedIndex(2);

                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };

        this.logTable.setRowHeight(22);
        TableColumnModel columnModel = logTable.getColumnModel();
        columnModel.getColumn(0).setMinWidth(30); columnModel.getColumn(0).setMaxWidth(80);
        columnModel.getColumn(1).setMinWidth(35); columnModel.getColumn(1).setMaxWidth(80);
        columnModel.getColumn(3).setMinWidth(35); columnModel.getColumn(3).setMaxWidth(180); columnModel.getColumn(3).setPreferredWidth(80);
        columnModel.getColumn(4).setMinWidth(30); columnModel.getColumn(4).setMaxWidth(180); columnModel.getColumn(4).setPreferredWidth(180);
        columnModel.getColumn(5).setMinWidth(30); columnModel.getColumn(5).setMaxWidth(180); columnModel.getColumn(5).setPreferredWidth(180);

        ColorChangingRenderer colorRenderer = new ColorChangingRenderer(tableModel);
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setCellRenderer(colorRenderer);
        }

        logTable.getColumnModel().getColumn(0).setHeaderRenderer(new DynamicHeaderRenderer(logTable.getTableHeader().getDefaultRenderer()));
        logTable.getColumnModel().getColumn(1).setHeaderRenderer(new DynamicHeaderRenderer(logTable.getTableHeader().getDefaultRenderer()));

        logTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isConfigPanelHidden()) return;
                int col = logTable.columnAtPoint(e.getPoint());
                if (col == 1) {
                    pressedHeaderColumn = col;
                    logTable.getTableHeader().repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isConfigPanelHidden()) return;
                pressedHeaderColumn = -1;
                logTable.getTableHeader().repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isConfigPanelHidden()) return;
                int col = logTable.columnAtPoint(e.getPoint());
                if (col == 0) {
                    performStartupAction();
                } else if (col == 1) {
                    performClearAction();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(logTable);
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();

        tableModel.addTableModelListener(e -> {
            SwingUtilities.invokeLater(() -> {
                int rowCount = logTable.getRowCount();
                if (rowCount == 0) return;
                boolean atBottom = (verticalScrollBar.getValue() + verticalScrollBar.getVisibleAmount() >= verticalScrollBar.getMaximum() - 20);
                if (atBottom || rowCount <= 1) {
                    SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
                }
            });
        });

        splitPane.setTopComponent(scrollPane);
        return splitPane;
    }

    class DynamicHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer defaultRenderer;

        public DynamicHeaderRenderer(TableCellRenderer defaultRenderer) {
            this.defaultRenderer = defaultRenderer;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isConfigPanelHidden()) {
                if (column == 0) {
                    if (autorizeStartupSwitch) {
                        return new FullRoundedLabel("ON", COLOR_BLUE_START, Color.WHITE);
                    } else {
                        return new FullRoundedLabel("OFF", COLOR_OFF_BG, Color.BLACK);
                    }
                }

                if (column == 1) {
                    if (pressedHeaderColumn == 1) {
                        return new FullRoundedLabel("Clear", COLOR_BLUE_START, Color.WHITE);
                    } else {
                        c.setBackground(UIManager.getColor("TableHeader.background"));
                        c.setForeground(UIManager.getColor("TableHeader.foreground"));
                        if (c instanceof JLabel) {
                            ((JLabel) c).setText("Clear");
                            ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                            ((JComponent) c).setBorder(UIManager.getBorder("TableHeader.cellBorder"));
                        }
                        return c;
                    }
                }
            }
            c.setBackground(UIManager.getColor("TableHeader.background"));
            c.setForeground(UIManager.getColor("TableHeader.foreground"));
            return c;
        }
    }

    static class FullRoundedLabel extends JLabel {
        private final Color bgColor;

        public FullRoundedLabel(String text, Color bgColor, Color fgColor) {
            super(text);
            this.bgColor = bgColor;
            setForeground(fgColor);
            setHorizontalAlignment(CENTER);
            setOpaque(false);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(UIManager.getColor("TableHeader.background"));
            g2.fillRect(0, 0, w, h);

            g2.setColor(bgColor);
            g2.fillRoundRect(1, 1, w - 2, h - 2, 12, 12);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class ColorChangingRenderer extends DefaultTableCellRenderer {
        private AutorizeTableModel model;

        public ColorChangingRenderer(AutorizeTableModel model) {
            this.model = model;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (column == 2) {
                setHorizontalAlignment(JLabel.LEFT);
            } else {
                setHorizontalAlignment(JLabel.CENTER);
            }

            LogEntry logEntry = model.get(row);
            Color backgroundColor = table.getBackground();

            boolean isVerticalBypass = logEntry.authBypassResponseLen == logEntry.originalResponseLen;
            boolean isUnauthBypass = logEntry.unauthResponseLen == logEntry.originalResponseLen;

            if (isVerticalBypass && !isUnauthBypass) {
                c.setForeground(Color.white);
                backgroundColor = COLOR_RED_ALERT;
            } else if (isVerticalBypass && isUnauthBypass) {
                c.setForeground(Color.white);
                backgroundColor = COLOR_ORANGE_ALERT;
            } else {
                c.setForeground(table.getForeground());
                backgroundColor = table.getBackground();
            }

            if (isSelected) {
                if (backgroundColor == table.getBackground()) {
                    backgroundColor = COLOR_SELECTION_BG;
                } else {
                    backgroundColor = backgroundColor.darker();
                }
            }
            c.setBackground(backgroundColor);
            return c;
        }
    }
}