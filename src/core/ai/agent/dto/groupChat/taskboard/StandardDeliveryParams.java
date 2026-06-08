package ai.agent.dto.groupChat.taskboard;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;

@Comment("标准交付任务参数")
@ClassDeclare(
        label = "标准交付任务参数",
        what = "标准交付任务的参数封装类", why = "用于类型安全的标准交付任务参数传递和重试", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2026-01-16", updateTime = "2026-01-16"
)
public class StandardDeliveryParams extends TaskExecutionParams {

    private String groupChatInstId;
    private List<String> panelCodes;
    private String userNeed;

    public StandardDeliveryParams() {
    }

    public StandardDeliveryParams(String groupChatInstId, List<String> panelCodes, String userNeed) {
        this.groupChatInstId = groupChatInstId;
        this.panelCodes = panelCodes;
        this.userNeed = userNeed;
    }

    @Override
    public TaskExecutionType getType() {
        return TaskExecutionType.STANDARD_DELIVERY;
    }

    // ========================= getter/setter =========================

    public String getGroupChatInstId() {
        return groupChatInstId;
    }

    public StandardDeliveryParams setGroupChatInstId(String groupChatInstId) {
        this.groupChatInstId = groupChatInstId;
        return this;
    }

    public List<String> getPanelCodes() {
        return panelCodes;
    }

    public StandardDeliveryParams setPanelCodes(List<String> panelCodes) {
        this.panelCodes = panelCodes;
        return this;
    }

    public String getUserNeed() {
        return userNeed;
    }

    public StandardDeliveryParams setUserNeed(String userNeed) {
        this.userNeed = userNeed;
        return this;
    }
}
