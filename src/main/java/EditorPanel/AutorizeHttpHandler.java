package EditorPanel;

import burp.api.montoya.http.handler.*;
import ui.ToolBoxUI;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static extension.ToolBox.api;


public class AutorizeHttpHandler implements HttpHandler {
    private final AutorizeTableModel tableModel;
    private ToolBoxUI toolsBoxUI;
    private List recordURLMD5 = new ArrayList();


    public AutorizeHttpHandler(AutorizeTableModel tableModel, ToolBoxUI toolsBoxUI) {
        this.tableModel = tableModel;
        this.toolsBoxUI = toolsBoxUI;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        checkVul(requestToBeSent);
        return RequestToBeSentAction.continueWith(requestToBeSent);

    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        tableModel.add(responseReceived);
        return ResponseReceivedAction.continueWith(responseReceived);
    }

    private HttpRequestToBeSent checkVul(HttpRequestToBeSent requestToBeSent) {
        Boolean whiteListSwitch = false;
        String fullURL = requestToBeSent.url();
        String url = fullURL.split("\\?")[0];
        // 开启白名单模式
        if (toolsBoxUI.isLoggingEnabled()) {
            for (String white_URL : ToolBoxUI.whiteListDomain) {
                if (url.contains(white_URL)) {
                    whiteListSwitch = true;
                    break;
                }
            }
            if (!whiteListSwitch) {
                return null;
            }
        }
        // 跳过静态资源
        String[] staticFile = {
                ".jpg", ".png", ".gif", ".css", ".js", ".pdf", ".mp3", ".mp4", ".avi", ".map",
                ".svg", ".ico", ".svg", ".woff", ".woff2", ".ttf"};
        for (String pass : staticFile) {
            if (url.endsWith(pass)) {
                return null;
            }
        }

        String method = requestToBeSent.method();
        String paramNames = requestToBeSent.parameters().stream().map(p -> p.name()).collect(Collectors.joining());
        String md5Hash = MD5Hash(url + paramNames + method);
        for (Object md5 : recordURLMD5) {
            if (md5.equals(md5Hash)) {
                return null;
            }
        }
        recordURLMD5.add(md5Hash);

        return requestToBeSent;

    }

    // MD5 计算方法
    private static String MD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error while calculating MD5", e);
        }
    }
}