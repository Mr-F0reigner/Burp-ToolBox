package Extension.Decoder;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import main.ToolBox;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private HttpRequestResponse requestResponse;
    private String currentEncoding = "GBK";
    private MontoyaApi api = ToolBox.api;
    // 创建编辑器面板
    private JPanel requestEditorUI = new JPanel(new BorderLayout());
    private JTextPane textPane;
    private JScrollPane scrollPane;
    private StyledDocument EditorDoc;
    private Style style;
    private String EditorMD5;
    private String[] JWTresult;
    private static final Pattern ModifiedJWT = Pattern.compile("(\\{.+?\\})(\\{.+?\\})([A-Za-z0-9-_]+)");

    RequestDecoder(EditorCreationContext creationContext) {
        // 将编辑器设置为可编辑模式
        requestEditor = api.userInterface().createRawEditor();

        // 创建编辑器面板样式（富文本，自动换行，下拉菜单）
        textPane = new JTextPane();     // 初始化 JTextPane 和 JScrollPane
        textPane.setContentType("text/html");  // 设置内容类型为 HTML，以支持富文本
        scrollPane = new JScrollPane(textPane);
        textPane.setEditorKit(new WrapEditorKit());     // 设置自动换行

        EditorDoc = textPane.getStyledDocument();
        style = textPane.addStyle("ColorStyle", null);

        // 创建 JScrollPane 并添加 JTextArea
        requestEditorUI.add(createDropdownMenu(), BorderLayout.NORTH); // 在顶部添加下拉菜单
        requestEditorUI.add(scrollPane, BorderLayout.CENTER); // 添加编辑器组件
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
        Boolean isModified = false;
        try {
            String currentMD5 = calculateMD5(EditorDoc.getText(0, EditorDoc.getLength()));
            isModified = (!currentMD5.equals(EditorMD5) && currentEncoding.equals("JWT"));
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
        return isModified;
    }

    /**
     * Raw面板获取数据包的操作。这里判断JWT如果被修改则执行更新操作
     */
    public HttpRequest getRequest() {
        HttpRequest request = requestResponse.request();
        try {
            String currentMD5 = calculateMD5(EditorDoc.getText(0, EditorDoc.getLength()));
            if (!currentMD5.equals(EditorMD5) && currentEncoding.equals("JWT")) {
                String modifiedText = textPane.getText().replaceAll("\r|\n|\s+", "");
                ;
                // 正则表达式来匹配 JWT 的三个部分
                Matcher matcher = ModifiedJWT.matcher(modifiedText);

                String modifiedHeader = "";
                String modifiedPayload = "";
                String modifiedSignature = "";

                if (matcher.find()) {
                    modifiedHeader = matcher.group(1); // 第一个匹配的是 Header
                    modifiedPayload = matcher.group(2); // 第二个匹配的是 Payload
                    modifiedSignature = matcher.group(3); // 第三个匹配的是 Signature
                }
                String encodedHeader = Base64.getUrlEncoder().encodeToString(modifiedHeader.getBytes(StandardCharsets.UTF_8));
                String encodedPayload = Base64.getUrlEncoder().encodeToString(modifiedPayload.getBytes(StandardCharsets.UTF_8));

                // 重构 JWT
                String rebuiltJWT = encodedHeader + "." + encodedPayload + "." + modifiedSignature;

                JWTHeaderValue = JWTHeaderValue.replace(JWTToken, rebuiltJWT);
                request = requestResponse.request().withUpdatedHeader(JWTHeaderKey, JWTHeaderValue);
            }
        } catch (Exception e) {
            api.logging().logToOutput("Error while processing JWT: " + e.getMessage());
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
        try {
            EditorDoc.remove(0, EditorDoc.getLength());
            if (findJWT) {
                String[] jwtTokenArr = decodeJWT(JWTToken);
                addColoredText(jwtTokenArr[0], Color.red);
                addColoredText(jwtTokenArr[1], Color.decode("#DE3AFF"));
                addColoredText(jwtTokenArr[2], Color.decode("#00C8F6"));
                EditorMD5 = calculateMD5(EditorDoc.getText(0, EditorDoc.getLength()));
            } else {
                addColoredText(encodeAndSetContent(currentEncoding), Color.black);
                EditorMD5 = calculateMD5(EditorDoc.getText(0, EditorDoc.getLength()));
            }
            // 设置滚动条默认处于顶部
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    scrollPane.getVerticalScrollBar().setValue(0);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 定义那些数据许需要进行处理。这里在检测到JWT执行指定操作。返回true表示处理所有数据。
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
     * 创建下拉菜单，定义菜单样式，菜单选项点击事件
     */
    // 修改后的createDropdownMenu方法
    private JComboBox<String> createDropdownMenu() {
        dropdown = new JComboBox<>();
        String[] encodings = {"JWT", "GBK", "GB2312", "GB18030", "UTF-8", "Big5", "Big5-HKSCS", "ISO-8859-1"};
        for (String encoding : encodings) {
            dropdown.addItem(encoding);
        }
        dropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setHorizontalAlignment(JLabel.CENTER);
                return label;
            }
        });

        dropdown.addActionListener(e -> {
            JComboBox<String> cb = (JComboBox<String>) e.getSource();
            String selectedEncoding = (String) cb.getSelectedItem();
            if ("JWT".equals(selectedEncoding)) {
                handleJWTDecoding();
            } else {
                setDecodedText(selectedEncoding);
            }
        });
        return dropdown;
    }

    private void setDecodedText(String encoding) {
        try {
            EditorDoc.remove(0, EditorDoc.getLength());
            currentEncoding = encoding;
            addColoredText(encodeAndSetContent(encoding), Color.black);
            SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }
    private void handleJWTDecoding() {
        try {
            EditorDoc.remove(0, EditorDoc.getLength());
            currentEncoding = "JWT";
            String[] jwtTokenArr = decodeJWT(JWTToken);
            addColoredText(jwtTokenArr[0], Color.red);
            addColoredText(jwtTokenArr[1], Color.decode("#DE3AFF"));
            addColoredText(jwtTokenArr[2], Color.decode("#00C8F6"));
            EditorMD5 = calculateMD5(EditorDoc.getText(0, EditorDoc.getLength()));
            SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String[] decodeJWT(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                return new String[]{"No JWT was found"};
            }

            // 解码 Header 和 Payload
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            // 将字符串转换为 JSON 对象以格式化输出
            JSONObject header = new JSONObject(headerJson);
            JSONObject payload = new JSONObject(payloadJson);

            // 将结果拆分为三个部分
            JWTresult = new String[3];
            JWTresult[0] = header.toString(4) + "\n\n"; // Header
            JWTresult[1] = payload.toString(4) + "\n\n"; // Payload
            JWTresult[2] = JWTSignature + "\n\n"; // Signature

            return JWTresult;
        } catch (Exception e) {
            return new String[]{"Error decoding JWT: " + e.getMessage()};
        }
    }

    /**
     * 根据指定的编码类型对数据进行解码后转换为UTF-8形式返回到编辑器中
     *
     * @param encoding 指定编码类型
     */
    private String encodeAndSetContent(String encoding) {
        ByteArray requestByteArray = requestResponse.request().toByteArray();

        // 使用指定的字符集编码进行解码
        String decodedRequest;
        try {
            decodedRequest = new String(requestByteArray.getBytes(), encoding);
        } catch (UnsupportedEncodingException e) {
            api.logging().logToOutput("Error: Unsupported Encoding for " + encoding);
            return "";
        }

        // 将解码后的字符串以UTF-8编码转换回字节数组，并设置到requestEditor
        byte[] utf8Bytes;
        try {
            utf8Bytes = decodedRequest.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            api.logging().logToOutput("Error: Unsupported UTF-8 Encoding");
            return "";
        }
//        requestEditor.setContents(ByteArray.byteArray(utf8Bytes));
        return decodedRequest;
    }

    private String calculateMD5(String input) {
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
            api.logging().logToOutput("Error calculating MD5: " + e.getMessage());
            return null;
        }
    }

    /**
     * 编辑器面板添加彩色文本的方法
     */
    private void addColoredText(String text, Color color) {
        StyleConstants.setForeground(style, color);
        try {
            EditorDoc.insertString(EditorDoc.getLength(), text, style);
        } catch (BadLocationException e) {
            api.logging().logToOutput("Error adding colored text: " + e.getMessage());
        }
    }

    /**
     * 自动换行的样式处理
     */
    static class WrapEditorKit extends StyledEditorKit {
        ViewFactory defaultFactory = new WrapColumnFactory();

        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
    }

    static class WrapColumnFactory implements ViewFactory {
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new WrapLabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new ParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }

            // 默认情况，不做特殊处理
            return new LabelView(elem);
        }
    }

    static class WrapLabelView extends LabelView {
        public WrapLabelView(Element elem) {
            super(elem);
        }

        public float getMinimumSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return 0;
                case View.Y_AXIS:
                    return super.getMinimumSpan(axis);
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }
    }
}