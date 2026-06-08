package ai.agent.dto.groupChat.message.payload;

import com.alibaba.fastjson2.annotation.JSONType;

/**
 * 智能体错误消息载荷
 * 用于智能体错误消息
 */

@JSONType(typeName = "AGENT_ERROR")
public class AgentErrorPayload extends MessagePayload {
    private String errorText;
    private String errorDetail;

    public AgentErrorPayload() {
    }

    public AgentErrorPayload(String errorText, String errorDetail) {
        this.errorText = errorText;
        this.errorDetail = errorDetail;

    }

    public String getErrorText() {
        return errorText;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public AgentErrorPayload setErrorText(String errorText) {
        this.errorText = errorText;
        return this;
    }

    public AgentErrorPayload setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
        return this;
    }

    @Override
    public String getPayloadType() {
        return "AGENT_ERROR";
    }

    @Override
    public boolean isValid() {
        return errorText != null && !errorText.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "AgentErrorPayload{" +
                "errorText='" + errorText + '\'' +
                ", errorDetail='" + errorDetail + '\'' +
                '}';
    }
}
