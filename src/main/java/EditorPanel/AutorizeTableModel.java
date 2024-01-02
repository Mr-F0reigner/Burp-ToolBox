package EditorPanel;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import extension.ToolBox;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ui.ToolBoxUI.authBypass;
import static ui.ToolBoxUI.unauthHeader;

public class AutorizeTableModel extends AbstractTableModel {
    private MontoyaApi api = ToolBox.api;
    private final List<HttpResponseReceived> log;

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
            case 4 -> responseReceived.body().copy();
            case 5 -> responseReceived.initiatingRequest().headers();
            default -> "";
        };
    }

    public synchronized void add(HttpResponseReceived responseReceived) {
        int index = log.size();
        log.add(responseReceived);
        fireTableRowsInserted(index, index);
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
}