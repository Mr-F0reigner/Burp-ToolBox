package Extension.Autorize;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import ui.Autorize;

import javax.swing.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static main.ToolBox.api;
import static ui.Autorize.*;

public class AutorizeHttpHandler implements HttpHandler {
    private final AutorizeTableModel tableModel;
    private Set<String> recordedUrlMD5 = AutorizeTableModel.recordedUrlMD5;

    public static AtomicInteger id = new AtomicInteger(0);

    // [优化 1] 引入线程池，避免无限创建线程导致 Burp 崩溃
    // 固定 20 个并发线程，既保证速度又不会卡死 UI
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(20);

    // [优化 2] 静态资源后缀改为 Set 集合，查找速度 O(1)
    // 且提取为静态常量，避免每次方法调用都重新创建数组
    private static final Set<String> STATIC_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".png", ".gif", ".css", ".js", ".pdf", ".mp3", ".mp4", ".avi", ".map",
            ".svg", ".ico", ".woff", ".woff2", ".ttf", ".jpeg", ".bmp", ".webp"
    ));

    public AutorizeHttpHandler(AutorizeTableModel tableModel) {
        this.tableModel = tableModel;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // 核心优化思路：能不进线程就不进线程，尽早在主流程拦截掉无效请求

        // 1. 基础判断
        if (!responseReceived.toolSource().isFromTool(ToolType.PROXY) || !autorizeStartupSwitch) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        HttpRequest request = responseReceived.initiatingRequest();
        String fullURL = request.url();
        String urlNoParams = fullURL.split("\\?")[0]; // 提取无参URL

        // 2. 忽略 OPTIONS
        if ("OPTIONS".equalsIgnoreCase(request.method())) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        // 3. 正则 URL 过滤 (优先执行，因为用户配置的通常最重要)
        if (Autorize.isUrlFiltered(fullURL)) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        // 4. [优化前置] 静态资源过滤
        // 提前到线程创建之前，大幅减少任务数
        String lowerUrl = urlNoParams.toLowerCase();
        for (String ext : STATIC_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) {
                return ResponseReceivedAction.continueWith(responseReceived);
            }
        }

        // 5. [优化前置] 白名单逻辑
        if (whiteListSwitch) {
            boolean inWhiteList = false;
            for (String allowDomain : whiteListDomain) {
                if (urlNoParams.contains(allowDomain)) {
                    inWhiteList = true;
                    break;
                }
            }
            if (!inWhiteList) {
                return ResponseReceivedAction.continueWith(responseReceived);
            }
        }

        // 6. [优化前置] MD5 去重
        // 计算 MD5 很快，比线程调度的开销小得多，所以也放在这里
        String method = request.method();
        // 注意：这里流式处理参数稍微有点耗时，但为了去重是值得的
        String paramNames = request.parameters().stream().map(p -> p.name()).collect(Collectors.joining());
        String md5Hash = MD5Hash(urlNoParams + paramNames + method);

        if (recordedUrlMD5.contains(md5Hash)) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }
        recordedUrlMD5.add(md5Hash); // 记录 MD5

        // 7. 通过所有检查，提交给线程池处理
        // 注意：这里传入 computed md5Hash 并没有用到，但如果逻辑需要可以传
        EXECUTOR.submit(() -> {
            try {
                // 传入 responseReceived 用于提取数据
                checkVul(responseReceived);
            } catch (Exception ex) {
                api.logging().logToError("CheckVul Error: " + request.url() + " -- " + ex.getMessage());
            }
        });

        return ResponseReceivedAction.continueWith(responseReceived);
    }

    /**
     * 核心检测逻辑 (在线程池中运行)
     */
    private void checkVul(HttpResponseReceived originalReqRes) {
        HttpRequest originalRequest = originalReqRes.initiatingRequest();
        String urlNoParams = originalRequest.url().split("\\?")[0];

        // 准备发送列表
        List<HttpRequest> scanRequestList = new ArrayList<>();

        // [优化 3] 移除 .copyToTempFile()
        // 直接基于内存对象操作，减少磁盘 IO
        HttpRequest authBypassRequest = originalRequest;
        HttpRequest unauthRequest = originalRequest;

        // 构造越权请求包
        for (String cert : Autorize.authBypass) {
            if (cert == null || !cert.contains(":")) continue;
            String[] parts = cert.split(":", 2);
            String certKey = parts[0].trim();
            String certValue = parts.length > 1 ? parts[1].trim() : "";

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

        scanRequestList.add(authBypassRequest);
        scanRequestList.add(unauthRequest);

        // 发送扫描请求 (网络 IO，线程池的作用体现于此)
        List<HttpRequestResponse> scanResponses = api.http().sendRequests(scanRequestList);

        if (scanResponses.size() < 2) return;

        HttpRequestResponse bypassReqRes = scanResponses.get(0);
        HttpRequestResponse unauthReqRes = scanResponses.get(1);

        // [优化 4] 确保 UI 更新在 EDT 线程中执行
        SwingUtilities.invokeLater(() -> {
            // 这里不需要再 synchronized(tableModel) 了，因为 invokeLater 保证了串行执行
            int currentId = id.incrementAndGet();
            tableModel.add(
                    currentId,
                    originalRequest.method(),
                    urlNoParams,
                    originalRequest,
                    originalReqRes,
                    originalReqRes.body().length(),
                    bypassReqRes.request(),
                    bypassReqRes.response(),
                    bypassReqRes.response().body().length(),
                    unauthReqRes.request(),
                    unauthReqRes.response(),
                    unauthReqRes.response().body().length()
            );
        });
    }

    // MD5 计算方法 (保持不变)
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