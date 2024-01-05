package Extension.Autorize;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public class LogEntry {
    public final int id;
    public final String method;
    public final String url;
    public final HttpRequest originalRequest;
    public final HttpResponse originalResponse;
    public final int originalResponseLen;
    public final HttpRequest authBypassRequest;
    public final HttpResponse authBypassResponse;
    public final int authBypassResponseLen;
    public final HttpRequest unauthRequest;
    public final HttpResponse unauthResponse;
    public final int unauthResponseLen;

    public LogEntry(int id, String method, String url, HttpRequest originalRequest, HttpResponse originalResponse, int originalResponseLen, HttpRequest authBypassRequest, HttpResponse authBypassResponse, int authBypassResponseLen, HttpRequest unauthRequest, HttpResponse unauthResponse, int unauthResponseLen) {
        this.id = id;
        this.method = method;
        this.url = url;
        this.originalRequest = originalRequest;
        this.originalResponse = originalResponse;
        this.originalResponseLen = originalResponseLen;
        this.authBypassRequest = authBypassRequest;
        this.authBypassResponse = authBypassResponse;
        this.authBypassResponseLen = authBypassResponseLen;
        this.unauthRequest = unauthRequest;
        this.unauthResponse = unauthResponse;
        this.unauthResponseLen = unauthResponseLen;
    }
}
