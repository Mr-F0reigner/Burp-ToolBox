package extension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import burp.api.montoya.MontoyaApi;
import org.json.JSONArray;

public class DNSLog {
    public static HttpURLConnection con;
    public static BufferedReader dnslogResponse;
//    public static String dnslogDomain;
    public static String dnslogSession;
    public MontoyaApi api = ToolBox.api;

    public DNSLog() {
        try {
            // 生成小数点前一位、小数点后16位的随机数
            double randomValue = Math.round(new Random().nextDouble() * Math.pow(10, 16)) / Math.pow(10, 16);

            // 以随机数作为参数的 URL
            String urlString = "http://dnslog.cn/getdomain.php?t=" + randomValue;
            URL url = new URL(urlString);

            // 发起请求
            con = (HttpURLConnection) url.openConnection();

            // 设置请求方式
            con.setRequestMethod("GET");

            // 获取响应包
            dnslogResponse = new BufferedReader(new InputStreamReader(con.getInputStream()));

            getDnslogSession();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDnslogDomain() throws IOException {
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = dnslogResponse.readLine()) != null) {
            content.append(inputLine);
        }
//        dnslogDomain = content.toString();
        return content.toString();
    }

    public void getDnslogSession() throws IOException {
        // Get and print the PHPSESSID value from the 'Set-Cookie' header
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
        api.logging().logToOutput(dnslogSession);
    }

    // Method to fetch DNS log records
    public JSONArray fetchDnsLogRecords() throws IOException {
        // 为请求生成随机数
        double randomValue = Math.round(new Random().nextDouble() * Math.pow(10, 16)) / Math.pow(10, 16);

        // 用于获取 DNS 日志记录的 URL
        String recordsUrl = "http://dnslog.cn/getrecords.php?t=" + randomValue;
        URL url = new URL(recordsUrl);

        // 建立连接
        HttpURLConnection recordsCon = (HttpURLConnection) url.openConnection();
        recordsCon.setRequestMethod("GET");

        // 将会话 cookie 添加到请求中
        String cookieValue = "PHPSESSID=" + dnslogSession;
        recordsCon.setRequestProperty("Cookie", cookieValue);

        // 阅读回复
        BufferedReader in = new BufferedReader(new InputStreamReader(recordsCon.getInputStream()));
        String inputLine;
        StringBuffer responseContent = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            responseContent.append(inputLine);
        }

        JSONArray jsonArray = new JSONArray(responseContent.toString());
        return jsonArray;


//        // 解析并打印响应
//        JSONArray jsonArray = new JSONArray(responseContent.toString());
////        System.out.println("DNS Query Record - IP Address - Created Time");
//        for (int i = 0; i < jsonArray.length(); i++) {
//            JSONArray record = jsonArray.getJSONArray(i);
//            System.out.println(record.getString(0) + " - " + record.getString(1) + " - " + record.getString(2));
//        }
    }
}