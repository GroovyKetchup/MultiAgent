package ai.agent.dto.groupChat.message.payload;

import com.alibaba.fastjson2.annotation.JSONType;

/**
 * 附件消息载荷
 * 结构：编号(id)、名称(name)
 */
@JSONType(typeName = "ATTACHMENT")
public class AttachmentPayload extends MessagePayload {
    private String fileCode;
    private
    String fileName;

    public AttachmentPayload() {
    }

    public AttachmentPayload(String id, String name) {
        this.fileCode = id;
        this.fileName = name;
    }

    public String getFileCode() {
        return fileCode;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String getPayloadType() {
        return "ATTACHMENT";
    }

    public AttachmentPayload setFileCode(String fileCode) {
        this.fileCode = fileCode;
        return this;
    }

    public AttachmentPayload setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Override
    public boolean isValid() {
        return fileCode != null && !fileCode.trim().isEmpty() && fileName != null && !fileName.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "AttachmentPayload{" +
                "id='" + fileCode + '\'' +
                ", name='" + fileName + '\'' +
                '}';
    }
}

