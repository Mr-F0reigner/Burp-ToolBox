package EditorPanel;

import burp.api.montoya.http.handler.*;

public class AutorizeHttpHandler implements HttpHandler
{
    private final AutorizeTableModel tableModel;

    public AutorizeHttpHandler(AutorizeTableModel tableModel)
    {
        this.tableModel = tableModel;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent)
    {
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived)
    {
        tableModel.add(responseReceived);
        return ResponseReceivedAction.continueWith(responseReceived);
    }
}