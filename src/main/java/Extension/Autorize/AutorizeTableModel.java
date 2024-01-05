package Extension.Autorize;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import main.ToolBox;

import javax.swing.table.AbstractTableModel;
import java.util.*;

public class AutorizeTableModel extends AbstractTableModel {
    private MontoyaApi api = ToolBox.api;
    private List<LogEntry> log;
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

    /**
     * 从日志模型中获取数据，为各列设置展示内容
     */
    @Override
    public synchronized Object getValueAt(int rowIndex, int columnIndex) {
        LogEntry logEntry = log.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return logEntry.id;
            case 1:
                return logEntry.method;
            case 2:
                return logEntry.url;
            case 3:
                return logEntry.originalResponseLen;
            case 4:
                if (logEntry.authBypassResponseLen == logEntry.originalResponseLen) {
                    return logEntry.originalResponseLen + " === " + logEntry.authBypassResponseLen + " ✔";
                } else {
                    return logEntry.originalResponseLen + " === " + logEntry.authBypassResponseLen;
                }
            case 5:
                if (logEntry.unauthResponseLen == logEntry.originalResponseLen) {
                    return logEntry.originalResponseLen + " === " + logEntry.unauthResponseLen + logEntry.unauthResponseLen + " ✔";
                } else {
                    return logEntry.originalResponseLen + " === " + logEntry.unauthResponseLen;
                }
            default:
                return "";
        }
    }


    /**
     * 将新增数据添加到日志模型中，并更新日志列表
     */
    public synchronized void add(int id, String method, String url, HttpRequest originalRequest, HttpResponse originalResponse, int originalResponseLen, HttpRequest authBypassRequest, HttpResponse authBypassResponse, int authBypassResponseLen, HttpRequest unauthRequest, HttpResponse unauthResponse, int unauthResponseLen) {
        int index = log.size();
        log.add(new LogEntry(id, method, url, originalRequest, originalResponse, originalResponseLen, authBypassRequest, authBypassResponse, authBypassResponseLen, unauthRequest, unauthResponse, unauthResponseLen));
        fireTableRowsInserted(index, index);
    }

    public synchronized LogEntry get(int rowIndex) {
        return log.get(rowIndex);
    }

    /**
     * 清空日志列表（自定义函数）
     */
    public synchronized void clearLog() {
        log.clear(); // 清空列表
        fireTableDataChanged(); // 通知数据变更
    }
}
