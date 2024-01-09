package Extension.Decoder;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.utilities.Base64Utils;
import burp.api.montoya.utilities.URLUtils;
import main.ToolBox;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对请求包中的URL和Body参数进行Base64解码
 */
class RequestDecoder implements ExtensionProvidedHttpRequestEditor {
    private Boolean findJWT = false;
    private String JWTToken;
    private String JWTHeaderKey;
    private String JWTHeaderValue;
    private String JWTSignature;
    private JComboBox<String> dropdown;
    private final RawEditor requestEditor;
    private final Base64Utils base64Utils;
    private final URLUtils urlUtils;
    private HttpRequestResponse requestResponse;
    private String currentEncoding = "GBK";
    private MontoyaApi api = ToolBox.api;
    // 创建编辑器面板
    private JPanel requestEditorUI = new JPanel(new BorderLayout());

    RequestDecoder(EditorCreationContext creationContext) {
        base64Utils = api.utilities().base64Utils();
        urlUtils = api.utilities().urlUtils();

        // 将编辑器设置为可编辑模式
        requestEditor = api.userInterface().createRawEditor();
        // 创建包含下拉菜单和编辑器组件的容器
        requestEditorUI.add(createDropdownMenu(), BorderLayout.NORTH); // 在顶部添加下拉菜单
        requestEditorUI.add(requestEditor.uiComponent(), BorderLayout.CENTER); // 添加编辑器组件
    }

    /**
     * Raw面板获取请求的操作。这里编辑器被设置为只读模式，所以Raw面板返回原始请求
     */
//        return requestResponse.request();
    @Override
    public HttpRequest getRequest() {
        HttpRequest request = requestResponse.request();
        if (requestEditor.isModified() && "JWT".equals(currentEncoding)) {
            try {
                // 将编辑器中的字节数组转换成字符串并格式化
                ByteArray contents = requestEditor.getContents();
                String modifiedContent = new String(contents.getBytes(), StandardCharsets.UTF_8);
                modifiedContent = modifiedContent.replaceAll("\r|\n|\s+", "");

                // 使用正则表达式提取 Headers 和 Payload
                Pattern pattern = Pattern.compile("(?<=Headers=).*(?=Payload)|(?<=Payload=).*(?=Signature)|(?<=Signature=\").*?(?=\")");
                Matcher matcher = pattern.matcher(modifiedContent);

                String modifiedHeader = "";
                String modifiedPayload = "";
                String modifiedSignature = "";
                if (matcher.find()) {
                    modifiedHeader = matcher.group(0);  // 第一个匹配的是 Header
                    if (matcher.find()) {
                        modifiedPayload = matcher.group(0);  // 第二个匹配的是 Payload
                        if (matcher.find()){
                            modifiedSignature = matcher.group(0);
                        }
                    }
                }

                String encodedHeader = Base64.getUrlEncoder().encodeToString(modifiedHeader.getBytes(StandardCharsets.UTF_8));
                String encodedPayload = Base64.getUrlEncoder().encodeToString(modifiedPayload.getBytes(StandardCharsets.UTF_8));

                // 重构 JWT
                String rebuiltJWT = encodedHeader + "." + encodedPayload + "." + modifiedSignature;

                JWTHeaderValue = JWTHeaderValue.replace(JWTToken, rebuiltJWT);
                request = requestResponse.request().withUpdatedHeader(JWTHeaderKey, JWTHeaderValue);

            } catch (Exception e) {
                api.logging().logToOutput("Error while processing JWT: " + e.getMessage());
            }
        }
        return request;
    }

    /**
     * 设置需要在编辑器中展示的内容
     *
     * @param requestResponse 要在编辑器中设置的请求和响应。
     */
    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        this.requestResponse = requestResponse;
    }

    /**
     * 定义那些数据许需要进行处理，返回true表示处理所有数据。检测到JWT执行指定操作
     *
     * @param requestResponse The {@link HttpRequestResponse} to check.
     */
    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        this.requestResponse = requestResponse;
        findJWT = false;
        try {
            Pattern jwtRegexp = Pattern.compile("ey[a-zA-Z0-9+/=]+\\.ey[a-zA-Z0-9+/=]+\\.?[a-zA-Z0-9-_]*$");
            List<HttpHeader> headers = requestResponse.request().headers();
            for (HttpHeader header : headers) {
                Matcher matcher = jwtRegexp.matcher(header.value());
                if (matcher.find()) {
                    JWTHeaderKey = header.name();
                    JWTHeaderValue = header.value();
                    JWTToken = matcher.group();
                    JWTSignature = JWTToken.substring(JWTToken.lastIndexOf(".") + 1);
                    currentEncoding = "JWT";
                    dropdown.setSelectedItem("JWT");
                    findJWT = true;
                    return true;
                }
            }
        } catch (Exception e) {
            ;
        }
        JWTToken = "";
        dropdown.setSelectedItem("GBK");
        return true;
    }

    /**
     * 设置编辑器标题名称
     */
    @Override
    public String caption() {
        return findJWT ? "JWT" : "Mr.F0reigner";
    }

    /**
     * 在消息编辑器选项卡中呈现的组件
     */
    @Override
    public Component uiComponent() {
        return requestEditorUI;
    }

    /**
     * 创建下拉菜单，定义菜单样式，菜单选项点击事件
     */
    private JComboBox<String> createDropdownMenu() {
        dropdown = new JComboBox<>();
        dropdown.addItem("JWT");
        dropdown.addItem("GBK");
        dropdown.addItem("GB2312");
        dropdown.addItem("GB18030");
        dropdown.addItem("UTF-8");
        dropdown.addItem("Big5");
        dropdown.addItem("Big5-HKSCS");
        dropdown.addItem("ISO-8859-1");
        // 设置 JComboBox 的渲染器，菜单控件文本居中
        dropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setHorizontalAlignment(JLabel.CENTER); // 菜单项文本居中
                return label;
            }
        });

        // 下拉菜单点击事件
        dropdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> cb = (JComboBox<String>) e.getSource();
                String selectedEncoding = (String) cb.getSelectedItem();
                switch (selectedEncoding) {
                    case "JWT":
                        currentEncoding = "JWT";
                        DecodeAsJWT();
                        break;
                    case "GBK":
                        currentEncoding = "GBK";
                        encodeAndSetContent("GBK");
                        break;
                    case "GB2312":
                        currentEncoding = "GB2312";
                        encodeAndSetContent("GB2312");
                        break;
                    case "GB18030":
                        currentEncoding = "GB18030";
                        encodeAndSetContent("GB18030");
                        break;
                    case "UTF-8":
                        currentEncoding = "UTF-8";
                        encodeAndSetContent("UTF-8");
                        break;
                    case "Big5":
                        currentEncoding = "Big5";
                        encodeAndSetContent("Big5");
                        break;
                    case "Big5-HKSCS":
                        currentEncoding = "Big5-HKSCS";
                        encodeAndSetContent("Big5-HKSCS");
                        break;
                    case "ISO-8859-1":
                        currentEncoding = "ISO-8859-1";
                        encodeAndSetContent("ISO-8859-1");
                        break;
                    default:
                        break;
                }
            }
        });
        return dropdown;
    }

    private void DecodeAsJWT() {
        if (!JWTToken.isEmpty()) {
            requestEditor.setContents(ByteArray.byteArray(decodeJWT(JWTToken)));
        } else {
            requestEditor.setContents(ByteArray.byteArray("""
                      _   _             _      _            _           _       ___        _______\s
                     | \\ | | ___     __| | ___| |_ ___  ___| |_ ___  __| |     | \\ \\      / /_   _|
                     |  \\| |/ _ \\   / _` |/ _ \\ __/ _ \\/ __| __/ _ \\/ _` |  _  | |\\ \\ /\\ / /  | | \s
                     | |\\  | (_) | | (_| |  __/ ||  __/ (__| ||  __/ (_| | | |_| | \\ V  V /   | | \s
                     |_| \\_|\\___/   \\__,_|\\___|\\__\\___|\\___|\\__\\___|\\__,_|  \\___/   \\_/\\_/    |_| \s
                    """));
        }
    }

    private String decodeJWT(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                return "Invalid JWT token format";
            }

            // 解码 Header 和 Payload
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            // 将字符串转换为 JSON 对象以格式化输出
            JSONObject header = new JSONObject(headerJson);
            JSONObject payload = new JSONObject(payloadJson);

            String result = "Headers = " + header.toString(4) + "\n\n" + "Payload = " + payload.toString(4) + "\n\n" + "Signature = \"" + JWTSignature + "\"";
            return result;
//            return header.toString(4) + "\n" + payload.toString(4); // 使用缩进为4的格式化输出
        } catch (Exception e) {
            return "Error decoding JWT: " + e.getMessage();
        }
    }

    /**
     * 根据指定的编码类型对数据进行解码后转换为UTF-8形式返回到编辑器中
     *
     * @param encoding 指定编码类型
     */
    private void encodeAndSetContent(String encoding) {
        ByteArray requestByteArray = requestResponse.request().toByteArray();

        // 使用指定的字符集编码进行解码
        String decodedRequest;
        try {
            decodedRequest = new String(requestByteArray.getBytes(), encoding);
        } catch (UnsupportedEncodingException e) {
            api.logging().logToOutput("Error: Unsupported Encoding for " + encoding);
            return;
        }

        // 将解码后的字符串以UTF-8编码转换回字节数组，并设置到requestEditor
        byte[] utf8Bytes;
        try {
            utf8Bytes = decodedRequest.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            api.logging().logToOutput("Error: Unsupported UTF-8 Encoding");
            return;
        }
        requestEditor.setContents(ByteArray.byteArray(utf8Bytes));
    }


    /**
     * 获取选中的数据。
     */
    @Override
    public Selection selectedData() {
        return requestEditor.selection().isPresent() ? requestEditor.selection().get() : null;
    }

    /**
     * 判断编辑器内容是否被修改。
     */
    @Override
    public boolean isModified() {
        return requestEditor.isModified();
    }
}