package EditorPanel;

import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.HttpRequestEditorProvider;

/**
 * 请求包解码器，这个是用于注册到Burp。{@link RequestDecoder}才是实现操作逻辑的类
 */
public class RequestDecoderProvider implements HttpRequestEditorProvider {
    @Override
    public ExtensionProvidedHttpRequestEditor provideHttpRequestEditor(EditorCreationContext creationContext) {
        return new RequestDecoder(creationContext);
    }
}
