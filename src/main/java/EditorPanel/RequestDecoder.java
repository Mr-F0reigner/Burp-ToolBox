package EditorPanel;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
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
import extension.ToolBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.ArrayList;

/**
 * 对请求包中的URL和Body参数进行Base64解码
 */
class RequestDecoder implements ExtensionProvidedHttpRequestEditor {
    private final RawEditor requestEditor;
    private final Base64Utils base64Utils;
    private final URLUtils urlUtils;
    private HttpRequestResponse requestResponse;
    private MontoyaApi api = ToolBox.api;

    private List<ParsedHttpParameter> parsedHttpParameter;

    // 构造函数，初始化编辑器和工具类。
    RequestDecoder(EditorCreationContext creationContext) {
        base64Utils = api.utilities().base64Utils();
        urlUtils = api.utilities().urlUtils();

        // 将编辑器设置为只读模式
        requestEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);
    }

    // Raw面板获取请求的操作。这里因为将编辑器设置为只读模式，所以Raw面板返回原始请求
    @Override
    public HttpRequest getRequest() {
        return requestResponse.request();
    }

    // 设置需要在编辑器中展示的内容。
    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        this.requestResponse = requestResponse;
        encodeAndSetContent("GBK");
    }

    // 确定此编辑器是否适用于特定的请求响应。
    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        // 返回是否找到参数。
        return true;
    }

    // 返回编辑器的标题。
    @Override
    public String caption() {
        return "Mr.F0reigner";
    }

    // 返回包含下拉菜单的编辑器UI组件
    @Override
    public Component uiComponent() {
        // 创建包含下拉菜单和编辑器组件的容器
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createDropdownMenu(), BorderLayout.NORTH); // 在顶部添加下拉菜单
        panel.add(requestEditor.uiComponent(), BorderLayout.CENTER); // 添加编辑器组件
        return panel;
    }

    // 创建一个下拉菜单
    private JComboBox<String> createDropdownMenu() {
        JComboBox<String> dropdown = new JComboBox<>();
        dropdown.addItem("GBK");
        dropdown.addItem("GB2312");
        dropdown.addItem("GB18030");
        dropdown.addItem("UTF-8");
        dropdown.addItem("Big5");
        dropdown.addItem("Big5-HKSCS");
        dropdown.addItem("ISO-8859-1");
        // 设置 JComboBox 的渲染器，以将文本居中
        dropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setHorizontalAlignment(JLabel.CENTER); // 居中对齐文本
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
                    case "GBK":
                        encodeAndSetContent("GBK");
                        break;
                    case "GB2312":
                        encodeAndSetContent("GB2312");
                        break;
                    case "GB18030":
                        encodeAndSetContent("GB18030");
                        break;
                    case "UTF-8":
                        encodeAndSetContent("UTF-8");
                        break;
                    case "Big5":
                        encodeAndSetContent("Big5");
                        break;
                    case "Big5-HKSCS":
                        encodeAndSetContent("Big5-HKSCS");
                        break;
                    case "ISO-8859-1":
                        encodeAndSetContent("ISO-8859-1");
                        break;
                    default:
                        break;
                }
            }
        });
        return dropdown;
    }

    // 指定编码操作
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


    // 获取选中的数据。
    @Override
    public Selection selectedData() {
        return requestEditor.selection().isPresent() ? requestEditor.selection().get() : null;
    }

    // 判断编辑器内容是否被修改。
    @Override
    public boolean isModified() {
        return requestEditor.isModified();
    }
}