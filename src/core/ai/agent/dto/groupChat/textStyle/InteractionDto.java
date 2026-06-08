package ai.agent.dto.groupChat.textStyle;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("交互消息Dto")
@ClassDeclare(
        label = "",
        what = "交互消息的完整定义", 
        why = "支持AI向用户发起简单交互请求", 
        how = "包含交互类型、提示文本、选项列表等",
        developer = "裴硕", 
        version = "1.0",
        createTime = "2025-12-14", 
        updateTime = "2025-12-14"
)
public class InteractionDto implements Serializable {
    
    @Comment("交互类型：options(选项列表)/confirm(确认对话框)/rating(评分)/yes_no(是否)")
    private String type;
    
    @Comment("提示文本")
    private String prompt;
    
    @Comment("详细内容，可选，用于confirm类型")
    private String content;
    
    @Comment("选项列表")
    private List<InteractionOption> options;
    
    @Comment("响应模板，默认为'选择了${option}'，${option}会被替换为用户选择的选项")
    private String responseTemplate;

    public String getType() {
        return type;
    }

    public InteractionDto setType(String type) {
        this.type = type;
        return this;
    }

    public String getPrompt() {
        return prompt;
    }

    public InteractionDto setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public String getContent() {
        return content;
    }

    public InteractionDto setContent(String content) {
        this.content = content;
        return this;
    }

    public List<InteractionOption> getOptions() {
        return options;
    }

    public InteractionDto setOptions(List<InteractionOption> options) {
        this.options = options;
        return this;
    }

    public String getResponseTemplate() {
        return responseTemplate;
    }

    public InteractionDto setResponseTemplate(String responseTemplate) {
        this.responseTemplate = responseTemplate;
        return this;
    }
}
