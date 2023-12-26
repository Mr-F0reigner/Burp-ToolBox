package extension;

import burp.api.montoya.proxy.ProxyHistoryFilter;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;

public class HttpHistory implements ProxyHistoryFilter {
    @Override
    public boolean matches(ProxyHttpRequestResponse requestResponse) {
        return false;
    }
}