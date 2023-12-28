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

public class DNSLog {
    private static HttpURLConnection con;
    private static String dnslogSession;
    private MontoyaApi api = ToolBox.api;

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
}