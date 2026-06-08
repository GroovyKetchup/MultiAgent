package ai.agent.constant;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("群聊常量")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-02", updateTime = "2025-09-02"
)
public class GroupChatConstants {

    // 默认群组
    public static final String DefaultGroupId = "default";
    // 默认群组实例Id
    public static final String DefaultGroupInstId = "default_inst";

    // LRU缓存大小 - 最多3个群聊实例
    public static final int MAX_GROUP_CHAT_INSTANCES = 5;

    // 群聊实例状态
    public static final String GROUP_CHAT_STATUS_ACTIVE = "ACTIVE";
    public static final String GROUP_CHAT_STATUS_INACTIVE = "INACTIVE";


    // ========================= 模型编号 =========================

    // 业务域编号
    public static final String BusDomainCode = "OctoCM_MutiAgentGroupChat";
    public static final String BusDomainCodeNoSystemPrefix = "MutiAgentGroupChat";


    // 组织模型ID
    public static final String OrgModelId = "gpf.md.org.OctoCM_MutiAgentGroupChat_Org";
    // 用户模型ID
    public static final String UserModelId = "gpf.md.user.OctoCM_MutiAgentGroupChat_User";

    // 群聊文件
    public static String FormModelId_GroupChatFile = "octocm.md.MutiAgentGroupChat.iML_00001_CM";
    // 群聊用户信息
    public static String FormModelId_GroupChatUserInfo = "octocm.md.MutiAgentGroupChat.iML_00003_CM";
    // 模型调用日志
    public static String FormModelId_ModelCallingLog = "octocm.md.MutiAgentGroupChat.iML_00004_CM";

    // 用户聊天记录
    public static String FormModelId_GroupChatUserChatRecord = "octocm.md.MutiAgentGroupChat.iML_00005_CM";

    // 群聊房间
    public static String FormModelId_GroupChatRoom = "octocm.md.MutiAgentGroupChat.iML_00006_CM";

    // 智能体定义
    public static String FormModelId_AgentDefinition = "octocm.md.MutiAgentGroupChat.iML_00010_CM";

    // 群聊引擎数据
    public static String FormModelId_GroupChatEngineData = "octocm.md.MutiAgentGroupChat.iML_00008_CM";

    // 智能体经验库
    public static String FormModelId_AgentExperienceLibrary = "octocm.md.MutiAgentGroupChat.iML_00011_CM";

    // 智能体经验内容
    public static String FormModelId_AgentExperienceContent= "octocm.md.MutiAgentGroupChat.slave.IML_00012_DataFM";

    // 系统设置
    public static String FormModelId_SystemSettings = "octocm.md.MutiAgentGroupChat.iML_00018_CM";


    // ========================= 标志位 =========================

    // 内部触发：在任务分配通知之后按顺序启动预制计划
    public static final String INTERNAL_PLAN_TRIGGER_PREFIX = "[[START_PREBUILT_PLAN:";

}
