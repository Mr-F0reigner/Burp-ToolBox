package EditorPanel;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.responses.HttpResponse;
import extension.ToolBox;

import javax.swing.table.AbstractTableModel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class AutorizeTableModel extends AbstractTableModel {
    private MontoyaApi api = ToolBox.api;
    private final List<HttpResponseReceived> log;
    public static Set<String> recordedUrlMD5 = new HashSet<>();

    public AutorizeTableModel() {
        this.log = new ArrayList<>();
    }

    @Override
    public synchronized int getRowCount() {
        return log.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "#";
            case 1 -> "类型";
            case 2 -> "URL";
            case 3 -> "原始包长度";
            case 4 -> "低权限包长度";
            case 5 -> "未授权包长度";
            default -> "";
        };
    }

    @Override
    public synchronized Object getValueAt(int rowIndex, int columnIndex) {
        HttpResponseReceived responseReceived = log.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> responseReceived.messageId();
            case 1 -> responseReceived.initiatingRequest().method();
            case 2 -> responseReceived.initiatingRequest().url();
            case 3 -> responseReceived.body().length();
            case 4 -> responseReceived.body().length(); // 更新为低权限响应长度
            case 5 -> responseReceived.body().length(); // 更新为未授权响应长度
            default -> "";
        };
    }

    public synchronized void add(HttpResponseReceived responseReceived) {
        if (shouldScanRequest(responseReceived)) {
            int index = log.size();
            log.add(responseReceived);
            fireTableRowsInserted(index, index);
        }
    }

    public synchronized HttpResponseReceived get(int rowIndex) {
        return log.get(rowIndex);
    }

    /**
     * 清空日志列表（自定义函数）
     */
    public synchronized void clearLog() {
        log.clear(); // 清空列表
        fireTableDataChanged(); // 通知数据变更
    }

    /**
     * 执行扫描的请求
     */
    private boolean shouldScanRequest(HttpResponseReceived responseReceived) {
        // 检查是否启用日志记录
        if (!ToolBox.toolsBoxUI.isLoggingEnabled()) {
            return false;
        }
        // 获取请求的URL
        String fullURL = responseReceived.initiatingRequest().url().toString();
        String url = fullURL.split("\\?")[0].trim();

        // 检查URL是否是静态资源
        String[] staticResourceExtensions = {"jpg", "png", "gif", "css", "js", "pdf", "mp3", "mp4", "avi", "map",
                "svg", "ico", "svg", "woff", "woff2", "ttf"};
        for (String extension : staticResourceExtensions) {
            if (url.toLowerCase().endsWith(extension)) {
                return false; // 如果是静态资源，不记录
            }
        }

        // 将URL、参数名和HTTP方法组合为一个字符串
        StringBuilder combinedString = new StringBuilder(url);
        for (ParsedHttpParameter parameter : responseReceived.initiatingRequest().parameters()) {
            combinedString.append("+").append(parameter.name());
        }
        combinedString.append("+").append(responseReceived.initiatingRequest().method());
        // 计算MD5哈希值
        String md5Hash = calculateMD5(combinedString.toString());
//        api.logging().logToOutput(String.valueOf(combinedString));
//        api.logging().logToOutput(md5Hash);

        // 如果不是静态资源，且未记录过该请求，则记录日志
        if (recordedUrlMD5.contains(md5Hash)) {
            return false;
        }else {
            recordedUrlMD5.add(md5Hash);
        }

        return true;
    }

    /**
     * 计算MD5
     * @param input URL+请求参数
     */
    public static String calculateMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String hex = Integer.toHexString(0xff & aMessageDigest);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}