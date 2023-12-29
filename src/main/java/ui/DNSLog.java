package ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import burp.api.montoya.MontoyaApi;
import extension.ToolBox;
import org.json.JSONArray;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class DNSLog {
    private static String dnslogSession;
    private JTextField domainTextField;
    private DefaultTableModel dnsLogModel;
    private JTable dataTable;
    private MontoyaApi api = ToolBox.api;

    public DNSLog(JTextField domainTextField, DefaultTableModel dnsLogModel, JTable dataTable) {
        this.domainTextField = domainTextField;
        this.dnsLogModel = dnsLogModel;
        this.dataTable = dataTable;
        initDNSLog();
    }

    /**
     * 初始化DNSLog选项卡
     */
    private void initDNSLog() {
        // 设置域名文本框样式
        domainTextField.setBorder(null);    // 无边框
        domainTextField.setOpaque(false);   // 透明背景
        // 定义列名
        final Object[] columnNames = {"DNS Query Record", "IP Address", "Created Time"};
        // 使用 DefaultTableModel，解决JTable不显示问题。设置初始数据为空
        dnsLogModel = new DefaultTableModel(columnNames, 0);
        dataTable.setModel(dnsLogModel);
        // 为每一列设置自定义渲染器，使数据居中显示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            dataTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    /**
     * 获取 DNSLog 域名
     *
     * @throws IOException
     */
    public void getDnslogDomain() throws IOException {
        // 清空解析记录面板
        while (dnsLogModel.getRowCount() > 0) {
            dnsLogModel.removeRow(0);
        }
        // 获取新域名
        URL url = new URL("http://dnslog.cn/getdomain.php?t=" + generateRandomValue());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        StringBuilder content = new StringBuilder();
        try (BufferedReader dnslogResponse = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = dnslogResponse.readLine()) != null) {
                content.append(inputLine);
            }
            domainTextField.setText(content.toString());

            // 获取域名对应的Session
            // 从“Set-Cookie”标头获取 PHPSESSID 值
            dnslogSession = "";
            String cookiesHeader = connection.getHeaderField("Set-Cookie");
            if (cookiesHeader != null) {
                String[] cookies = cookiesHeader.split("; ");
                for (String cookie : cookies) {
                    if (cookie.startsWith("PHPSESSID")) {
                        dnslogSession = cookie.split("=")[1];
                    }
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 刷新解析记录
     */
    public void refreshRecordAction() {
        try {
            JSONArray jsonArray = fetchDnsLogRecords();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray record = jsonArray.getJSONArray(i);
                // 从 record 中获取需要检查的数据
                String data1 = record.getString(0); // 第一列
                String data2 = record.getString(1); // 第二列
                String data3 = record.getString(2); // 第三列
                // 检查数据是否已经存在于 dnsLogModel 中
                boolean dataExists = false;
                for (int j = 0; j < dnsLogModel.getRowCount(); j++) {
                    if (dnsLogModel.getValueAt(j, 0).equals(data1) &&
                            dnsLogModel.getValueAt(j, 1).equals(data2) &&
                            dnsLogModel.getValueAt(j, 2).equals(data3)) {
                        dataExists = true;
                        break;
                    }
                }
                // 如果数据不存在，则添加到 dnsLogModel
                if (!dataExists) {
                    dnsLogModel.addRow(new Object[]{data1, data2, data3});
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    // 获取DNS解析记录
    public JSONArray fetchDnsLogRecords() throws IOException {
        String recordsUrl = "http://dnslog.cn/getrecords.php?t=" + generateRandomValue();
        URL url = new URL(recordsUrl);
        HttpURLConnection recordsCon = (HttpURLConnection) url.openConnection();
        recordsCon.setRequestMethod("GET");

        // 将域名对应的Session添加到Cookie中
        String cookieValue = "PHPSESSID=" + dnslogSession;
        recordsCon.setRequestProperty("Cookie", cookieValue);

        BufferedReader in = new BufferedReader(new InputStreamReader(recordsCon.getInputStream()));
        String inputLine;
        StringBuffer responseContent = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            responseContent.append(inputLine);
        }
        in.close();

        // 解析 JSON 数据
        JSONArray jsonArray = new JSONArray(responseContent.toString());
        return jsonArray;
    }

    /**
     * 获取随机数
     *
     * @return
     */
    private double generateRandomValue() {
        return Math.round(new Random().nextDouble() * Math.pow(10, 16)) / Math.pow(10, 16);
    }
}