package ai.agent.dto.groupChat.admin;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("平台使用情况")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-16", updateTime = "2025-09-16"
)
public class PlatformUsageSituationDto implements Serializable {

    // 用户名称
    private String userName;
    // 企业名称
    private String enterpriseName;
    // 手机号
    private String phone;
    // 当前模型调用次数
    private Long currentModelCallingNo;
    // 最大模型调用次数
    private Long maxModelCallingNo;
    // 用户文档分析次数
    private Long documentAnalysesNo;
    // 用户上传的文件编号
    private List<UserDocumentDto> userDocumentFile;
    // 用户发送的信息
    private List<String> userSentMessages;
    // 上次活跃时间
    private Long lastActiveTime;

    public String getUserName() {
        return userName;
    }

    public PlatformUsageSituationDto setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getEnterpriseName() {
        return enterpriseName;
    }

    public PlatformUsageSituationDto setEnterpriseName(String enterpriseName) {
        this.enterpriseName = enterpriseName;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public PlatformUsageSituationDto setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public Long getCurrentModelCallingNo() {
        return currentModelCallingNo;
    }

    public PlatformUsageSituationDto setCurrentModelCallingNo(Long currentModelCallingNo) {
        this.currentModelCallingNo = currentModelCallingNo;
        return this;
    }

    public Long getMaxModelCallingNo() {
        return maxModelCallingNo;
    }

    public PlatformUsageSituationDto setMaxModelCallingNo(Long maxModelCallingNo) {
        this.maxModelCallingNo = maxModelCallingNo;
        return this;
    }

    public Long getDocumentAnalysesNo() {
        return documentAnalysesNo;
    }

    public PlatformUsageSituationDto setDocumentAnalysesNo(Long documentAnalysesNo) {
        this.documentAnalysesNo = documentAnalysesNo;
        return this;
    }

    public List<UserDocumentDto> getUserDocumentFile() {
        return userDocumentFile;
    }

    public PlatformUsageSituationDto setUserDocumentFile(List<UserDocumentDto> userDocumentFile) {
        this.userDocumentFile = userDocumentFile;
        return this;
    }

    public List<String> getUserSentMessages() {
        return userSentMessages;
    }

    public PlatformUsageSituationDto setUserSentMessages(List<String> userSentMessages) {
        this.userSentMessages = userSentMessages;
        return this;
    }

    public Long getLastActiveTime() {
        return lastActiveTime;
    }

    public PlatformUsageSituationDto setLastActiveTime(Long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
        return this;
    }
}
