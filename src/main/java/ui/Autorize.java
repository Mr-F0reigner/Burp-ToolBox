package ui;

import burp.api.montoya.MontoyaApi;
import extension.ToolBox;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Autorize {
    private MontoyaApi api = ToolBox.api;
    public static DefaultTableModel autorizeModel;
    private JSplitPane authVerticalSplitPane;
    private JSplitPane authhorizontalSplitPane;
    private JPanel authorityVulnPanel;
    private JPanel authorityConfigPanel;
    private JPanel whiteListPanel;
    private JPanel authorityPanel;

    public Autorize(JSplitPane authVerticalSplitPane, JSplitPane authhorizontalSplitPane, JPanel authorityVulnPanel, JPanel authorityConfigPanel, JPanel whiteListPanel, JPanel authorityPanel) {
        this.authVerticalSplitPane = authVerticalSplitPane;
        this.authhorizontalSplitPane = authhorizontalSplitPane;
        this.authorityVulnPanel = authorityVulnPanel;
        this.authorityConfigPanel = authorityConfigPanel;
        this.whiteListPanel = whiteListPanel;
        this.authorityPanel = authorityPanel;
        initAutorize();
    }

    private void initAutorize() {
    }


}
