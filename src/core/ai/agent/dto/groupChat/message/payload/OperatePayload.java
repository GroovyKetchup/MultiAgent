package ai.agent.dto.groupChat.message.payload;

import com.alibaba.fastjson2.annotation.JSONType;

/**
 * 操作消息载荷
 * 用于通知前端执行特定操作
 */
@JSONType(typeName = "OPERATE")
public class OperatePayload extends MessagePayload {
    private String operateName; // 操作名称
    private Object operateData; // 操作携带的数据

    public OperatePayload() {
    }

    public OperatePayload(String operateName, Object operateData) {
        this.operateName = operateName;
        this.operateData = operateData;
    }

    public String getOperateName() {
        return operateName;
    }

    public Object getOperateData() {
        return operateData;
    }

    public OperatePayload setOperateName(String operateName) {
        this.operateName = operateName;
        return this;
    }

    public OperatePayload setOperateData(Object operateData) {
        this.operateData = operateData;
        return this;
    }

    @Override
    public String getPayloadType() {
        return "OPERATE";
    }

    @Override
    public boolean isValid() {
        return operateName != null && !operateName.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "OperatePayload{" +
                "operateName='" + operateName + '\'' +
                ", operateData=" + operateData +
                '}';
    }
}
