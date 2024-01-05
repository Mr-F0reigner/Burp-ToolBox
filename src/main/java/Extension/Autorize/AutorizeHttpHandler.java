package Extension.Autorize;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import ui.Autorize;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static main.ToolBox.api;
import static ui.Autorize.*;


public class AutorizeHttpHandler implements HttpHandler {
    private final AutorizeTableModel tableModel;
    private Set<String> recordedUrlMD5 = AutorizeTableModel.recordedUrlMD5;

    public AtomicInteger id = new AtomicInteger(0);

    public AutorizeHttpHandler(AutorizeTableModel tableModel) {
        this.tableModel = tableModel;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        if (requestToBeSent.toolSource().isFromTool(ToolType.PROXY) && autorizeStartupSwitch) {
            Thread thread = new Thread(() -> {
                try {
                    checkVul(requestToBeSent);
                } catch (Exception ex) {
                    api.logging().logToOutput(requestToBeSent.url() + "--" + ex.getMessage());
                }
            });
            thread.start();
        }
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        return ResponseReceivedAction.continueWith(responseReceived);
    }

    private void checkVul(HttpRequestToBeSent requestToBeSent) {
        String fullURL = requestToBeSent.url();
        String url = fullURL.split("\\?")[0];
        Boolean inWhiteList = false;

        if (whiteListSwitch) {
            for (String allowDomain : whiteListDomain) {
                if (url.contains(allowDomain)) {
                    inWhiteList = true;
                    break;
                }
            }
            if (!inWhiteList) {
                return;
            }
        }

        // 跳过静态资源
        String[] staticFile = {
                ".jpg", ".png", ".gif", ".css", ".js", ".pdf", ".mp3", ".mp4", ".avi", ".map",
                ".svg", ".ico", ".svg", ".woff", ".woff2", ".ttf"};
        for (String pass : staticFile) {
            if (url.endsWith(pass)) {
                return;
            }
        }
        String method = requestToBeSent.method();
        String paramNames = requestToBeSent.parameters().stream().map(p -> p.name()).collect(Collectors.joining());
        String md5Hash = MD5Hash(url + paramNames + method);
        for (Object md5 : recordedUrlMD5) {
            if (md5.equals(md5Hash)) {
                return;
            }
        }
        recordedUrlMD5.add(md5Hash);

        List<HttpRequestResponse> localResponseList = new ArrayList<>();
        List<HttpRequest> localRequestList = new ArrayList<>();
        HttpRequest authBypassRequest = requestToBeSent.copyToTempFile().withService(requestToBeSent.httpService());
        HttpRequest unauthRequest = requestToBeSent.copyToTempFile().withService(requestToBeSent.httpService());
        // 构造越权请求包
        for (String cert : Autorize.authBypass) {
            String certKey = cert.split(":")[0].trim();
            String certValue = cert.split(":")[1].trim();
            if (authBypassRequest.hasHeader(certKey)) {
                authBypassRequest = authBypassRequest.withUpdatedHeader(certKey, certValue);
            }else{
                authBypassRequest = authBypassRequest.withAddedHeader(certKey, certValue);
            }
        }
        // 构造未授权请求包
        for (String cert : Autorize.unauthHeader) {
            if (unauthRequest.hasHeader(cert)) {
                unauthRequest = unauthRequest.withRemovedHeader(cert);
            }
        }
        localRequestList.add(requestToBeSent);
        localRequestList.add(authBypassRequest);
        localRequestList.add(unauthRequest);
        List<HttpRequestResponse> httpRequestResponses = api.http().sendRequests(localRequestList);
        for (HttpRequestResponse httpRequestResponse : httpRequestResponses) {
            localResponseList.add(httpRequestResponse);
        }
        synchronized (tableModel) {
            // 使用id.incrementAndGet()获取当前id并自增
            int currentId = id.incrementAndGet();
            tableModel.add(currentId, requestToBeSent.method(), requestToBeSent.url().split("\\?")[0], localResponseList.get(0).request(), localResponseList.get(0).response(), localResponseList.get(0).response().body().length(), localResponseList.get(1).request(), localResponseList.get(1).response(), localResponseList.get(1).response().body().length(), localResponseList.get(2).request(), localResponseList.get(2).response(), localResponseList.get(2).response().body().length());
        }
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