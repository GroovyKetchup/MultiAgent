package ai.agent.dto.groupChat.textStyle;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("交互选项")
@ClassDeclare(
        label = "",
        what = "交互消息的单个选项定义", 
        why = "支持AI生成用户交互选择", 
        how = "定义选项的显示文本",
        developer = "裴硕", 
        version = "1.0",
        createTime = "2025-12-14", 
        updateTime = "2025-12-14"
)
public class InteractionOption implements Serializable {
    
    @Comment("选项文本，显示给用户，同时作为变量名")
    private String label;

    public String getLabel() {
        return label;
    }

    public InteractionOption setLabel(String label) {
        this.label = label;
        return this;
    }
}
