package ai.agent.enums;

import java.util.Locale;

public enum UserRole {
    // 管理员
    ADMIN("admin", "/平台初始化组织/管理员"),
    // 平台成员
    MEMBER("member", "/平台初始化组织/普通成员");


    public final String roleName;
    public final String rolePath;

    UserRole(String roleName, String rolePath) {
        this.roleName = roleName;
        this.rolePath = rolePath;
    }

    public static UserRole fromRoleName(String roleName) {
        roleName = roleName.toLowerCase(Locale.ROOT);
        for (UserRole type : UserRole.values()) {
            if (type.roleName.equals(roleName)) {
                return type;
            }
        }
        return null;
    }


}
