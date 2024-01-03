package EditorPanel;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import extension.ToolBox;

import java.util.Locale;

import static extension.ToolBox.api;
import static ui.ToolBoxUI.authBypass;
import static ui.ToolBoxUI.unauthHeader;


public class AutorizeHttpHandler implements HttpHandler {
    private final AutorizeTableModel tableModel;
    private volatile boolean isInternalRequest = false;


    public AutorizeHttpHandler(AutorizeTableModel tableModel) {
        this.tableModel = tableModel;
    }
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        tableModel.add(responseReceived);
        return ResponseReceivedAction.continueWith(responseReceived);
    }
}