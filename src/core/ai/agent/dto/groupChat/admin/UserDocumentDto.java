package ai.agent.dto.groupChat.admin;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("用户上传的文档")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-16", updateTime = "2025-09-16"
)
public class UserDocumentDto implements Serializable {
    private String docCode;
    private String docName;

    public String getDocCode() {
        return docCode;
    }

    public UserDocumentDto setDocCode(String docCode) {
        this.docCode = docCode;
        return this;
    }

    public String getDocName() {
        return docName;
    }

    public UserDocumentDto setDocName(String docName) {
        this.docName = docName;
        return this;
    }
}
