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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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

    public static Autorize instance;

    // [保持原样] 继续使用 Boolean，不引入 AtomicBoolean
    public static Boolean whiteListSwitch = false;
    public static Boolean autorizeStartupSwitch = false;

    // 列表保持 CopyOnWriteArrayList (这是之前的优化，建议保留)
    public static java.util.List<String> whiteListDomain = new CopyOnWriteArrayList<>();
    public static java.util.List<String> unauthHeader = new CopyOnWriteArrayList<>();
    public static List<String> authBypass = new CopyOnWriteArrayList<>();

    private static final int MIN_CONFIG_PANEL_WIDTH = 350;

    // ================== [优化点 2] 提取颜色常量 ==================
    // 避免在渲染循环中重复创建 Color 对象，大幅提升滚动流畅度
    private static final Color COLOR_RED_ALERT = Color.decode("#FF6464");
    private static final Color COLOR_ORANGE_ALERT = Color.decode("#FF8C00");
    private static final Color COLOR_BLUE_START = Color.decode("#26649D");
    private static final Color COLOR_GRAY_BG = Color.decode("#F5F5F5");
    private static final Color COLOR_GRAY_TEXT = Color.decode("#888888");
    private static final Color COLOR_TEXT_DEFAULT = Color.decode("#2B2D30");
    private static final Color COLOR_TEXT_HINT = Color.decode("#8C8C8C");
    private static final Color COLOR_SELECTION_BG = Color.decode("#CADAF0");

    private static final String WHITELIST_HINT = "如果需要多个域名加白请用逗号隔开";

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

    public void updateAuthBypassContent(String content) {
        SwingUtilities.invokeLater(() -> {
            this.authBypassTextArea.setText(content);
            // [优化点 3 应用] 使用通用方法更新列表
            if (autorizeStartupSwitch) {
                updateListFromText(authBypass, content, "\n");
            }
        });
    }

    public void updateUnauthContent(String content) {
        SwingUtilities.invokeLater(() -> {
            this.unauthTextArea.setText(content);
            // [优化点 3 应用] 使用通用方法更新列表
            if (autorizeStartupSwitch) {
                updateListFromText(unauthHeader, content, "\n");
            }
        });
    }

    /**
     * ================== [优化点 3] 提取通用列表更新逻辑 ==================
     * 消除重复代码，使用 Stream API 简化处理
     */
    private void updateListFromText(List<String> targetList, String text, String separator) {
        targetList.clear();
        if (text == null || text.isEmpty()) return;

        List<String> newItems = Arrays.stream(text.split(separator))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        targetList.addAll(newItems);
    }

    private void toggleConfigPanel() {
        int totalWidth = authVerticalSplitPane.getWidth();
        int dividerSize = authVerticalSplitPane.getDividerSize();
        int currentLoc = authVerticalSplitPane.getDividerLocation();
        int currentRightWidth = totalWidth - currentLoc - dividerSize;
        boolean isHidden = currentRightWidth < 50;

        if (isHidden) {
            authorityConfigPanel.setMinimumSize(new Dimension(MIN_CONFIG_PANEL_WIDTH, 0));
            int targetLoc = totalWidth - dividerSize - MIN_CONFIG_PANEL_WIDTH;
            if (targetLoc < 0) targetLoc = totalWidth / 2;
            authVerticalSplitPane.setDividerLocation(targetLoc);
        } else {
            authorityConfigPanel.setMinimumSize(new Dimension(0, 0));
            authVerticalSplitPane.setDividerLocation(1.0);
        }
    }

    private void autorizeActionListener() {
        startupButton.addActionListener(e -> {
            autorizeStartupSwitch = !autorizeStartupSwitch;

            if (autorizeStartupSwitch) {
                startupButton.setText("Autorize is On");
                startupButton.setBackground(COLOR_BLUE_START); // 使用常量
                startupButton.setForeground(Color.white);

                // [优化点 3 应用]
                updateListFromText(authBypass, authBypassTextArea.getText(), "\n");

                authBypassTextArea.setEditable(false);
                authBypassTextArea.setBackground(COLOR_GRAY_BG); // 使用常量
                authBypassTextArea.setForeground(COLOR_GRAY_TEXT); // 使用常量

                unauthTextArea.setEnabled(false);

                // [优化点 3 应用]
                updateListFromText(unauthHeader, unauthTextArea.getText(), "\n");
            } else {
                startupButton.setText("Autorize is Off");
                startupButton.setBackground(null);
                startupButton.setForeground(null);

                authBypassTextArea.setEditable(true);
                authBypassTextArea.setBackground(Color.WHITE);
                authBypassTextArea.setForeground(Color.BLACK);

                unauthTextArea.setEnabled(true);
            }
        });

        clearListButton.addActionListener(e -> {
            tableModel.clearLog();
            AutorizeTableModel.recordedUrlMD5.clear();
            AutorizeHttpHandler.id.set(0);
        });

        startupWhiteListButton.addActionListener(e -> {
            whiteListSwitch = !whiteListSwitch;

            if (whiteListSwitch) {
                whiteListTextField.setEnabled(false);
                startupWhiteListButton.setText("关闭白名单");
                startupWhiteListButton.setBackground(COLOR_BLUE_START);
                startupWhiteListButton.setForeground(Color.white);

                // [优化点 3 应用]
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

        JTable table = new JTable(tableModel) {
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                LogEntry logEntry = tableModel.get(rowIndex);

                // ================== [优化点 4] 优化 changeSelection ==================
                // 1. 统一设置数据，消除 switch 中的 6 行重复代码
                originalRequest.setRequest(logEntry.originalRequest);
                originalResponse.setResponse(logEntry.originalResponse);
                lowAuthRequest.setRequest(logEntry.authBypassRequest);
                lowAuthResponse.setResponse(logEntry.authBypassResponse);
                unauthRequest.setRequest(logEntry.unauthRequest);
                unauthResponse.setResponse(logEntry.unauthResponse);

                // 2. 仅处理 Tab 切换逻辑
                if (columnIndex == 3) tabs.setSelectedIndex(0);
                else if (columnIndex == 4) tabs.setSelectedIndex(1);
                else if (columnIndex == 5) tabs.setSelectedIndex(2);

                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setMinWidth(30); columnModel.getColumn(0).setMaxWidth(80);
        columnModel.getColumn(1).setMinWidth(35); columnModel.getColumn(1).setMaxWidth(80);
        columnModel.getColumn(3).setMinWidth(35); columnModel.getColumn(3).setMaxWidth(180); columnModel.getColumn(3).setPreferredWidth(80);
        columnModel.getColumn(4).setMinWidth(30); columnModel.getColumn(4).setMaxWidth(180); columnModel.getColumn(4).setPreferredWidth(180);
        columnModel.getColumn(5).setMinWidth(30); columnModel.getColumn(5).setMaxWidth(180); columnModel.getColumn(5).setPreferredWidth(180);

        // 使用优化后的渲染器
        ColorChangingRenderer colorRenderer = new ColorChangingRenderer(tableModel);
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setCellRenderer(colorRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);

        // 智能自动滚动逻辑
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        tableModel.addTableModelListener(e -> {
            SwingUtilities.invokeLater(() -> {
                int rowCount = table.getRowCount();
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

    /**
     * [优化点 2 应用] 渲染器性能优化
     */
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

            // 使用预定义颜色常量
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