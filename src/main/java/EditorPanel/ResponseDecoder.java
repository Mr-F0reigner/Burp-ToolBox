package EditorPanel;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.EditorMode;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.utilities.Base64EncodingOptions;
import burp.api.montoya.utilities.Base64Utils;
import burp.api.montoya.utilities.URLUtils;
import extension.ToolBox;

import java.awt.*;

public class ResponseDecoder implements ExtensionProvidedHttpResponseEditor {
    private MontoyaApi api = ToolBox.api;
    private final RawEditor responseEditor;
    private final Base64Utils base64Utils;
    private final URLUtils urlUtils;
    private HttpRequestResponse requestResponse;

    public ResponseDecoder(EditorCreationContext creationContext) {
        base64Utils = api.utilities().base64Utils();
        urlUtils = api.utilities().urlUtils();

        if (creationContext.editorMode() == EditorMode.READ_ONLY) {
            responseEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);
        } else {
            responseEditor = api.userInterface().createRawEditor();
        }
    }

    @Override
    public HttpResponse getResponse() {
        HttpResponse  response;

        if (responseEditor.isModified()) {
            // reserialize data
            String base64Encoded = base64Utils.encodeToString(responseEditor.getContents(), Base64EncodingOptions.URL);
            String encodedData = urlUtils.encode(base64Encoded);

            response = requestResponse.response().withBody(encodedData);
        } else {
            response = requestResponse.response();
        }

        return response;
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {

    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        return false;
    }

    @Override
    public String caption() {
        return null;
    }

    @Override
    public Component uiComponent() {
        return null;
    }

    @Override
    public Selection selectedData() {
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
