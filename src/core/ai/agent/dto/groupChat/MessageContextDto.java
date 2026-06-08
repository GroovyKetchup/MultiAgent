package ai.agent.dto.groupChat;

import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.dto.llmCalling.LlmUsageStats;
import ai.agent.dto.llmCalling.ToolDto;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("消息上下文DTO")
@ClassDeclare(
        label = "消息上下文",
        what = "记录每条消息发送时的完整上下文信息", 
        why = "用于调试和分析智能体的决策过程", 
        how = "包含系统提示词、可用工具列表和消息历史",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-15", updateTime = "2025-12-15"
)
public class MessageContextDto implements Serializable {

    private String systemPrompt;
    private List<ToolDto> toolList;
    private List<LlmMessage> msgList;
    private Long timestamp;
    private Integer loopIndex;
    private List<LlmUsageStats> usageStatsList;

    public MessageContextDto() {
    }

    public MessageContextDto(String systemPrompt, List<ToolDto> toolList, List<LlmMessage> msgList) {
        this.systemPrompt = systemPrompt;
        this.toolList = toolList;
        this.msgList = msgList;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public MessageContextDto setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    public List<ToolDto> getToolList() {
        return toolList;
    }

    public MessageContextDto setToolList(List<ToolDto> toolList) {
        this.toolList = toolList;
        return this;
    }

    public List<LlmMessage> getMsgList() {
        return msgList;
    }

    public MessageContextDto setMsgList(List<LlmMessage> msgList) {
        this.msgList = msgList;
        return this;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public MessageContextDto setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Integer getLoopIndex() {
        return loopIndex;
    }

    public MessageContextDto setLoopIndex(Integer loopIndex) {
        this.loopIndex = loopIndex;
        return this;
    }

    public List<LlmUsageStats> getUsageStatsList() {
        return usageStatsList;
    }

    public MessageContextDto setUsageStatsList(List<LlmUsageStats> usageStatsList) {
        this.usageStatsList = usageStatsList;
        return this;
    }
}
