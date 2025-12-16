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

    public static AtomicInteger id = new AtomicInteger(0);

    public AutorizeHttpHandler(AutorizeTableModel tableModel) {
        this.tableModel = tableModel;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        // [修改 1] 移除这里的扫描逻辑，只做转发
        // 我们不需要在请求发出前拦截，因为那时还没有收到原始响应
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // [修改 2] 将逻辑移到这里：收到响应后触发
        // 这样我们可以直接利用已经产生的原始请求和响应，而不需要重发
        if (responseReceived.toolSource().isFromTool(ToolType.PROXY) && autorizeStartupSwitch) {
            Thread thread = new Thread(() -> {
                try {
                    // 传入当前收到的完整请求/响应对象
                    checkVul(responseReceived);
                } catch (Exception ex) {
                    api.logging().logToOutput(responseReceived.initiatingRequest().url() + "--" + ex.getMessage());
                }
            });
            thread.start();
        }
        return ResponseReceivedAction.continueWith(responseReceived);
    }

    /**
     * 核心检测逻辑
     * @param originalReqRes 原始的请求响应对象 (浏览器发出的那个)
     */
    private void checkVul(HttpResponseReceived originalReqRes) {
        // 获取原始请求对象
        HttpRequest originalRequest = originalReqRes.initiatingRequest();

        String fullURL = originalRequest.url();
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
                ".svg", ".ico", ".woff", ".woff2", ".ttf"}; // 删除了重复的 .svg
        for (String pass : staticFile) {
            // [小优化] 忽略大小写判断后缀，防止 .PNG 漏网
            if (url.toLowerCase().endsWith(pass)) {
                return;
            }
        }

        String method = originalRequest.method();
        String paramNames = originalRequest.parameters().stream().map(p -> p.name()).collect(Collectors.joining());
        String md5Hash = MD5Hash(url + paramNames + method);

        // MD5 去重
        if (recordedUrlMD5.contains(md5Hash)) {
            return;
        }
        recordedUrlMD5.add(md5Hash);

        // [修改 3] 准备发送列表：只包含越权和未授权请求，不包含原始请求
        List<HttpRequest> scanRequestList = new ArrayList<>();

        // 基于原始请求复制
        HttpRequest authBypassRequest = originalRequest.copyToTempFile().withService(originalRequest.httpService());
        HttpRequest unauthRequest = originalRequest.copyToTempFile().withService(originalRequest.httpService());

        // 构造越权请求包
        for (String cert : Autorize.authBypass) {
            // 增加安全判断防止空指针
            if (cert == null || !cert.contains(":")) continue;
            String certKey = cert.split(":")[0].trim();
            // 防止 Value 为空的情况
            String certValue = cert.split(":").length > 1 ? cert.split(":")[1].trim() : "";

            if (authBypassRequest.hasHeader(certKey)) {
                authBypassRequest = authBypassRequest.withUpdatedHeader(certKey, certValue);
            } else {
                authBypassRequest = authBypassRequest.withAddedHeader(certKey, certValue);
            }
        }

        // 构造未授权请求包
        for (String cert : Autorize.unauthHeader) {
            if (unauthRequest.hasHeader(cert)) {
                unauthRequest = unauthRequest.withRemovedHeader(cert);
            }
        }

        // 添加到发送列表 (注意：这里不再添加 originalRequest)
        scanRequestList.add(authBypassRequest);
        scanRequestList.add(unauthRequest);

        // [修改 4] 发送扫描请求
        // 这里的 responses 只会包含 2 个结果：Index 0 是越权响应，Index 1 是未授权响应
        List<HttpRequestResponse> scanResponses = api.http().sendRequests(scanRequestList);

        if (scanResponses.size() < 2) return; // 简单防护

        HttpRequestResponse bypassReqRes = scanResponses.get(0);
        HttpRequestResponse unauthReqRes = scanResponses.get(1);

        // [修改 5] 入库逻辑调整
        synchronized (tableModel) {
            int currentId = id.incrementAndGet();
            tableModel.add(
                    currentId,
                    originalRequest.method(),
                    url,
                    // 1. 原始请求/响应：直接使用传入的 originalReqRes，这是浏览器真实收到的
                    originalRequest,
                    originalReqRes,
                    originalReqRes.body().length(),
                    // 2. 越权请求/响应：来自扫描结果 Index 0
                    bypassReqRes.request(),
                    bypassReqRes.response(),
                    bypassReqRes.response().body().length(),
                    // 3. 未授权请求/响应：来自扫描结果 Index 1
                    unauthReqRes.request(),
                    unauthReqRes.response(),
                    unauthReqRes.response().body().length()
            );
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