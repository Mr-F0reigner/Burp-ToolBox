package ui;

import javax.swing.*;

public class ToolBoxUI {
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
    private JButton startupButton;
    private JScrollPane authBypassScrollPane;
    private JScrollPane unauthScrollPane;
    private JLabel unauthLabel;


    public ToolBoxUI() {
        new DNSLog(domainTextField, dataTable,getSubDomain,refreshRecord);
        new ConfigTab(configTable, configScrollPane,saveBotton);
        new Autorize(authorityVulnPanel,authVerticalSplitPane,authhorizontalSplitPane,authorityConfigPanel,whiteListPanel,authorityPanel,startupButton,authBypassTextArea,unauthTextArea,clearListButton,startupWhiteListButton,whiteListTextField);
    }

}
