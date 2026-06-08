package ai.agent.dto.groupChat.taskboard;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;

@Comment("个性交付任务参数")
@ClassDeclare(
        label = "个性交付任务参数",
        what = "个性交付任务的参数封装类", why = "用于类型安全的个性交付任务参数传递和重试", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2026-01-16", updateTime = "2026-01-16"
)
public class CustomDeliveryParams extends TaskExecutionParams {

    private String groupChatInstId;
    private List<String> panelCodes;
    private String userNeed;

    public CustomDeliveryParams() {
    }

    public CustomDeliveryParams(String groupChatInstId, List<String> panelCodes, String userNeed) {
        this.groupChatInstId = groupChatInstId;
        this.panelCodes = panelCodes;
        this.userNeed = userNeed;
    }

    @Override
    public TaskExecutionType getType() {
        return TaskExecutionType.CUSTOM_DELIVERY;
    }

    // ========================= getter/setter =========================

    public String getGroupChatInstId() {
        return groupChatInstId;
    }

    public CustomDeliveryParams setGroupChatInstId(String groupChatInstId) {
        this.groupChatInstId = groupChatInstId;
        return this;
    }

    public List<String> getPanelCodes() {
        return panelCodes;
    }

    public CustomDeliveryParams setPanelCodes(List<String> panelCodes) {
        this.panelCodes = panelCodes;
        return this;
    }

    public String getUserNeed() {
        return userNeed;
    }

    public CustomDeliveryParams setUserNeed(String userNeed) {
        this.userNeed = userNeed;
        return this;
    }
}
