package Extension.Decoder;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.utilities.Base64Utils;
import burp.api.montoya.utilities.URLUtils;
import main.ToolBox;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

public class ResponseDecoder implements ExtensionProvidedHttpResponseEditor {
    private MontoyaApi api = ToolBox.api;
    private final RawEditor responseEditor;
    private HttpRequestResponse requestResponse;
    private JPanel responseEditorUI = new JPanel(new BorderLayout());
    private String currentEncoding = "GBK";
    private JTextPane textPane;
    private JScrollPane scrollPane;
    private JComboBox<String> dropdown;
    private StyledDocument EditorDoc;
    private Style style;

    ResponseDecoder(EditorCreationContext creationContext) {
        // 将编辑器设置为可编辑模式
        responseEditor = api.userInterface().createRawEditor();

        // 创建编辑器面板样式（富文本，自动换行，下拉菜单）
        textPane = new JTextPane();     // 初始化 JTextPane 和 JScrollPane
        textPane.setContentType("text/html");  // 设置内容类型为 HTML，以支持富文本
        scrollPane = new JScrollPane(textPane);
        textPane.setEditorKit(new RequestDecoder.WrapEditorKit());     // 设置自动换行

        EditorDoc = textPane.getStyledDocument();
        style = textPane.addStyle("ColorStyle", null);

        // 创建 JScrollPane 并添加 JTextArea
        responseEditorUI.add(createDropdownMenu(), BorderLayout.NORTH); // 在顶部添加下拉菜单
        responseEditorUI.add(scrollPane, BorderLayout.CENTER); // 添加编辑器组件
    }

    /**
     * Raw面板获取请求的操作。这里编辑器被设置为只读模式，所以Raw面板返回原始请求
     *
     * @return
     */
    @Override
    public HttpResponse getResponse() {
        return requestResponse.response();
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
            addColoredText(encodeAndSetContent(currentEncoding), Color.black);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    scrollPane.getVerticalScrollBar().setValue(0);
                }
            });
        } catch (Exception e) {
            ;
        }
    }

    /**
     * 定义那些数据许需要进行处理，返回true表示处理所有数据
     */
    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        // 返回是否找到参数。
        return true;
    }

    /**
     * 设置编辑器标题名称
     */
    @Override
    public String caption() {
        return "Mr.F0reigner";
    }

    /**
     * 在消息编辑器选项卡中呈现的组件
     */
    @Override
    public Component uiComponent() {
        return responseEditorUI;
    }

    /**
     * 创建下拉菜单，定义菜单样式，菜单选项点击事件
     */
    // 修改后的createDropdownMenu方法
    private JComboBox<String> createDropdownMenu() {
        dropdown = new JComboBox<>();
        String[] encodings = {"GBK", "GB2312", "GB18030", "UTF-8", "Big5", "Big5-HKSCS", "ISO-8859-1"};
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
            setDecodedText(selectedEncoding);
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

    /**
     * 根据指定的编码类型对数据进行解码后转换为UTF-8形式返回到编辑器中
     *
     * @param encoding 指定编码类型
     */
    private String encodeAndSetContent(String encoding) {
        ByteArray requestByteArray = requestResponse.response().toByteArray();

        // 使用指定的字符集编码进行解码
        String decodedRequest;
        try {
            decodedRequest = new String(requestByteArray.getBytes(), encoding);
        } catch (UnsupportedEncodingException e) {
            api.logging().logToOutput("Error: Unsupported Encoding for " + encoding);
            return "";
        }
        return decodedRequest;
    }


    /**
     * 获取选中的数据。
     *
     * @return
     */
    @Override
    public Selection selectedData() {
        return responseEditor.selection().isPresent() ? responseEditor.selection().get() : null;
    }

    /**
     * 判断编辑器内容是否被修改。
     *
     * @return
     */
    @Override
    public boolean isModified() {
        return responseEditor.isModified();
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
}
