> [BurpSuite 插件开发-Montoya Api - 1ndex- - 博客园 (cnblogs.com)](https://www.cnblogs.com/wjrblogs/p/16921644.html)

# 0x01  环境部署

- **Java：19**

- **导入Montoya Api包到src/main/java中**

# 0x02  要点记录
- 设置JTable表单的时候需要套一层JScrollPane解决不显示问题

# 0x03  参考笔记
### 01. 将请求包中URL和Body类型的参数Base64解码后传入编辑器面板

```java
package Extension;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.main.EditorCreationContext;
import burp.api.montoya.ui.editor.main.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.utilities.Base64Utils;
import burp.api.montoya.utilities.URLUtils;
import main.ToolBox;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 对请求包中的URL和Body参数进行Base64解码
 */
class RequestDecoder implements ExtensionProvidedHttpRequestEditor {
    private final RawEditor requestEditor;
    private final Base64Utils base64Utils;
    private final URLUtils urlUtils;
    private HttpRequestResponse requestResponse;
    private MontoyaApi api = ToolBox.api;

    private List<ParsedHttpParameter> parsedHttpParameter = new ArrayList<>();

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

        HttpRequest updateRequest = this.requestResponse.request();

        ByteArray editorOutput;
        // 遍历参数列表
        for (ParsedHttpParameter param : parsedHttpParameter) {
            // URL解码
            String urlDecoded = urlUtils.decode(param.value());
            // 使用异常捕获实现base64解码失败时输出原字符，反之更新请求参数
            try {
                editorOutput = base64Utils.decode(urlDecoded);
                if (param.type() == HttpParameterType.URL) {
                    updateRequest = updateRequest.withUpdatedParameters(HttpParameter.parameter(param.name(), editorOutput.toString(), param.type()));
                } else if (param.type() == HttpParameterType.BODY) {
                    updateRequest = updateRequest.withUpdatedParameters(HttpParameter.parameter(param.name(), editorOutput.toString(), param.type()));
                }
            } catch (Exception e) {
                updateRequest = updateRequest.withUpdatedParameters(HttpParameter.parameter(param.name(), urlDecoded, param.type()));
            }

            // 将更新后的请求包内容写入到编辑器中
            this.requestEditor.setContents(updateRequest.toByteArray());
        }
    }

    // 确定此编辑器是否适用于特定的请求响应。
    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        parsedHttpParameter.clear();
        // 过滤提取所有来自URL和Body中的参数
        List<ParsedHttpParameter> paramList = requestResponse.request().parameters().stream().filter(p -> p.type() == HttpParameterType.URL || p.type() == HttpParameterType.BODY).collect(Collectors.toList());

        // 将过滤后的参数添加到全局变量中
        for (ParsedHttpParameter param : paramList) {
            parsedHttpParameter.add(param);
        }

        // 返回是否找到参数。
        return true;
    }

    // 返回编辑器的标题。
    @Override
    public String caption() {
        return "Mr.F0reigner";
    }

    // 返回编辑器的UI组件。
    @Override
    public Component uiComponent() {
        return requestEditor.uiComponent();
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
```