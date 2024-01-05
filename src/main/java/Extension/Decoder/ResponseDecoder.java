package Extension.Decoder;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.utilities.Base64Utils;
import burp.api.montoya.utilities.URLUtils;
import main.ToolBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class ResponseDecoder implements ExtensionProvidedHttpResponseEditor {
    private final RawEditor responseEditor;
    private final Base64Utils base64Utils;
    private final URLUtils urlUtils;
    private HttpRequestResponse requestResponse;
    private MontoyaApi api = ToolBox.api;

    private List<ParsedHttpParameter> parsedHttpParameter;
    private JPanel responseEditorUI = new JPanel(new BorderLayout());
    private String currentEncoding = "GBK";

    ResponseDecoder(EditorCreationContext creationContext) {
        base64Utils = api.utilities().base64Utils();
        urlUtils = api.utilities().urlUtils();

        // 将编辑器设置为只读模式
        responseEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);
        responseEditorUI.add(createDropdownMenu(), BorderLayout.NORTH); // 在顶部添加下拉菜单
        responseEditorUI.add(responseEditor.uiComponent(), BorderLayout.CENTER); // 添加编辑器组件
    }

    /**
     * Raw面板获取请求的操作。这里编辑器被设置为只读模式，所以Raw面板返回原始请求
     * @return
     */
    @Override
    public HttpResponse getResponse() {
        return requestResponse.response();
    }

    /**
     * 设置需要在编辑器中展示的内容
     * @param requestResponse 要在编辑器中设置的请求和响应。
     */
    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        try {
            this.requestResponse = requestResponse;
            encodeAndSetContent(currentEncoding);
        } catch (Exception e) {
            ;
        }
    }

    /**
     * 定义那些数据许需要进行处理，返回true表示处理所有数据
     * @param requestResponse The {@link HttpRequestResponse} to check.
     *
     * @return
     */
    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        // 返回是否找到参数。
        return true;
    }

    /**
     * 设置编辑器标题名称
     * @return
     */
    @Override
    public String caption() {
        return "Mr.F0reigner";
    }

    /**
     * 在消息编辑器选项卡中呈现的组件
     * @return
     */
    @Override
    public Component uiComponent() {

        return responseEditorUI;
    }

    /**
     * 创建下拉菜单，定义菜单样式，菜单选项点击事件
     * @return
     */
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

    /**
     * 根据指定的编码类型对数据进行解码后转换为UTF-8形式返回到编辑器中
     * @param encoding 指定编码类型
     */
    private void encodeAndSetContent(String encoding) {
        ByteArray responseByteArray = requestResponse.response().toByteArray();

        // 使用指定的字符集编码进行解码
        String decodedResponse;
        try {
            decodedResponse = new String(responseByteArray.getBytes(), encoding);
        } catch (UnsupportedEncodingException e) {
            api.logging().logToOutput("Error: Unsupported Encoding for " + encoding);
            return;
        }

        // 将解码后的字符串以UTF-8编码转换回字节数组，并设置到requestEditor
        byte[] utf8Bytes;
        try {
            utf8Bytes = decodedResponse.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            api.logging().logToOutput("Error: Unsupported UTF-8 Encoding");
            return;
        }
        responseEditor.setContents(ByteArray.byteArray(utf8Bytes));
    }


    /**
     * 获取选中的数据。
     * @return
     */
    @Override
    public Selection selectedData() {
        return responseEditor.selection().isPresent() ? responseEditor.selection().get() : null;
    }

    /**
     * 判断编辑器内容是否被修改。
     * @return
     */
    @Override
    public boolean isModified() {
        return responseEditor.isModified();
    }
}
