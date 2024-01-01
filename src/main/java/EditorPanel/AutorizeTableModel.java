package EditorPanel;

import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class AutorizeTableModel extends AbstractTableModel
{
    private final List<HttpResponseReceived> log;

    public AutorizeTableModel()
    {
        this.log = new ArrayList<>();
    }

    @Override
    public synchronized int getRowCount()
    {
        return log.size();
    }

    @Override
    public int getColumnCount()
    {
        return 6;
    }

    @Override
    public String getColumnName(int column)
    {
        return switch (column)
        {
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
    public synchronized Object getValueAt(int rowIndex, int columnIndex)
    {
        HttpResponseReceived responseReceived = log.get(rowIndex);
        HttpRequest originalrequest = responseReceived.initiatingRequest();
        HttpResponse modifiedResponse = null;


        return switch (columnIndex)
        {
            case 0 -> responseReceived.messageId();
            case 1 -> responseReceived.initiatingRequest().method();
            case 2 -> responseReceived.initiatingRequest().url();
            case 3 -> responseReceived.body().length();
            case 4 -> responseReceived.initiatingRequest().url();
            case 5 -> responseReceived.initiatingRequest().url();
            default -> "";
        };
    }

    public synchronized void add(HttpResponseReceived responseReceived)
    {
        int index = log.size();
        log.add(responseReceived);
        fireTableRowsInserted(index, index);
    }

    public synchronized HttpResponseReceived get(int rowIndex)
    {
        return log.get(rowIndex);
    }
}