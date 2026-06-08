package ai.agent.dto.groupChat.message.payload;

import com.alibaba.fastjson2.annotation.JSONType;

/**
 * 文本消息载荷
 * 用于普通文本消息
 */
@JSONType(typeName = "TEXT")
public class TextPayload extends MessagePayload {


    // 文本
    private String text;
    // 样式 @see TextPayloadStyle
    private String style;

    public TextPayload(String text) {
        this.text = text;
    }

    public TextPayload(String text, String style) {
        this.text = text;
        this.style = style;
    }

    public String getText() {
        return text;
    }

    public TextPayload setText(String text) {
        this.text = text;
        return this;
    }

    public String getStyle() {
        return style;
    }

    public TextPayload setStyle(String style) {
        this.style = style;
        return this;
    }

    @Override
    public String getPayloadType() {
        return "TEXT";
    }

    @Override
    public boolean isValid() {
        return text != null && !text.trim().isEmpty();
    }


    @Override
    public String toString() {
        return "TextPayload{text='" + text + "'}";
    }
}
