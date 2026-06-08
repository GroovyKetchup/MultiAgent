package ai.agent.dto.groupChat.textStyle;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("省略消息Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-12", updateTime = "2025-12-12"
)
public class OmittedTextDto implements Serializable {
    private String title;
    private String detail;


    public OmittedTextDto() {
    }

    public OmittedTextDto(String title, String detail) {
        this.title = title;
        this.detail = detail;
    }

    public String getTitle() {
        return title;
    }

    public OmittedTextDto setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDetail() {
        return detail;
    }

    public OmittedTextDto setDetail(String detail) {
        this.detail = detail;
        return this;
    }
}
