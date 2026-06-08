package ai.agent.dto.groupChat;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("系统成员Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-11-04", updateTime = "2025-11-04"
)
public class SystemMemberDto {
    // 用户名
    private String userName;
    // 全名（中文名）
    private String fullName;
    // 角色名称
    private String roleName;

    public SystemMemberDto() {
    }

    public SystemMemberDto(String userName, String fullName, String roleName) {
        this.userName = userName;
        this.fullName = fullName;
        this.roleName = roleName;
    }

    public String getUserName() {
        return userName;
    }

    public SystemMemberDto setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public SystemMemberDto setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getRoleName() {
        return roleName;
    }

    public SystemMemberDto setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }
}

