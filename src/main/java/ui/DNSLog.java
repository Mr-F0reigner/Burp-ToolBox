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
    private static HttpURLConnection con;
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
        // 使用 DefaultTableModel，解决JTable不显示问题。设置初始为空
        dnsLogModel = new DefaultTableModel(columnNames, 0);
        dataTable.setModel(dnsLogModel);
        // 为每一列设置自定义渲染器，使数据居中显示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            dataTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    public void getSubDomainAction(){
        try {
            while (dnsLogModel.getRowCount() > 0) {
                dnsLogModel.removeRow(0);
            }
//            domainTextField.setText(dnsLog.getDnslogDomain());
            domainTextField.setText(getDnslogDomain());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void refreshRecordAction() {
        try {
//            JSONArray jsonArray = dnsLog.fetchDnsLogRecords();
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


    /**
     * 获取随机数
     * @return
     */
    private double generateRandomValue() {
        return Math.round(new Random().nextDouble() * Math.pow(10, 16)) / Math.pow(10, 16);
    }

    /**
     * 发起HTTP请求
     * @param urlString
     * @return
     * @throws IOException
     */
    private HttpURLConnection createConnection(String urlString) throws IOException {
        // 指定URL
        URL url = new URL(urlString);
        // 发起http请求
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // 设置请求方式
        connection.setRequestMethod("GET");
        return connection;
    }

    /**
     * 获取 DNSLog 域名
     * @return
     * @throws IOException
     */
    public String getDnslogDomain() throws IOException {
        this.con = createConnection("http://dnslog.cn/getdomain.php?t=" + generateRandomValue());
        StringBuilder content = new StringBuilder();
        try (BufferedReader dnslogResponse = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = dnslogResponse.readLine()) != null) {
                content.append(inputLine);
            }
        } finally {
            con.disconnect();
        }
        getDnslogSession();
        return content.toString();
    }

    /**
     * 获取域名对应的Session
     * @throws IOException
     */
    public void getDnslogSession() throws IOException {
        // 从“Set-Cookie”标头获取 PHPSESSID 值
        String getSession = "";
        String cookiesHeader = con.getHeaderField("Set-Cookie");
        if (cookiesHeader != null) {
            String[] cookies = cookiesHeader.split("; ");
            for (String cookie : cookies) {
                if (cookie.startsWith("PHPSESSID")) {
                    getSession = cookie.split("=")[1];
                }
            }
        }
        dnslogSession = getSession;
    }

    // Method to fetch DNS log records
    public JSONArray fetchDnsLogRecords() throws IOException {
        // 用于获取 DNS 日志记录的 URL
        String recordsUrl = "http://dnslog.cn/getrecords.php?t=" + generateRandomValue();
        URL url = new URL(recordsUrl);

        // 建立连接
        HttpURLConnection recordsCon = (HttpURLConnection) url.openConnection();
        recordsCon.setRequestMethod("GET");

        // 将会话 cookie 添加到请求中
        String cookieValue = "PHPSESSID=" + dnslogSession;
        recordsCon.setRequestProperty("Cookie", cookieValue);

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(recordsCon.getInputStream()));
        } catch (IOException e) {
            api.logging().logToError(e.getMessage());
        }
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
}