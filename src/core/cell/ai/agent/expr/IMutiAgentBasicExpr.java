package cell.ai.agent.expr;

import ai.agent.constant.GroupChatConstants;
import ai.agent.dto.groupChat.SystemMemberDto;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.model.definition.SystemSettingsDefinition;
import ai.agent.enums.ThreadPoolType;
import ai.agent.enums.UserRole;
import ai.agent.service.GroupChatThreadPollManager;
import ai.agent.service.groupChat.manager.GroupDefinitionManager;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatEngineStoreUtil;
import cell.CellIntf;
import cell.ai.agent.IGroupChatMessageService;
import cell.ai.agent.IGroupChatRoomService;
import cell.ai.agent.IVotaForgeService;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.gpf.adur.role.IRoleMgr;
import cell.gpf.adur.user.IUserMgr;
import cell.gpf.dc.runtime.IDCRuntimeContext;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cmn.http.util.HttpSessionUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.Password;
import gpf.adur.data.ResultSet;
import gpf.adur.role.Role;
import gpf.adur.user.User;
import gpf.dc.http.AppUserInfo;
import octo.cm.util.EasyOperation;
import octo.cm.util.FormToJsonConversionUtil;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Comment("多智能体群聊规则")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-17", updateTime = "2025-09-17"
)
public interface IMutiAgentBasicExpr extends CellIntf {

    EasyOperation Op = EasyOperation.get();

    @MethodDeclare(
            label = "获取成员列表", how = "", what = "", why = "",
            inputs = {}
    )
    default List<SystemMemberDto> getMemberList() throws Exception {
        try (IDao dao = IDaoService.newIDao()) {
            OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(GroupChatConstants.BusDomainCode);
            if (observer == null) throw new RuntimeException("无法找到对应的业务域");

            ResultSet<User> userResultSet = IUserMgr.get().queryUserPage(dao, GroupChatConstants.UserModelId, null, 1, 10, true);

            List<SystemMemberDto> members = new ArrayList<>();
            Set<String> existedCodes = new HashSet<>();
            for (UserRole userRole : UserRole.values()) {
                Role targetRole = Op.getOrCreateSimpleRole(dao, observer, GroupChatConstants.OrgModelId,
                        userRole.rolePath, "成员");
                if (targetRole == null) continue;

                List<User> users = IRoleMgr.get().queryMountedUserList(dao, targetRole.getUuid(), GroupChatConstants.UserModelId);
                if (Op.isEmpty(users)) continue;

                for (User user : users) {
                    if (existedCodes.contains(user.getCode())) continue;
                    existedCodes.add(user.getCode());
                    members.add(
                            new SystemMemberDto().setUserName(user.getUserName())
                                    .setFullName(user.getFullName())
                                    .setRoleName(userRole.roleName)
                    );
                }

            }


            return members;
        }
    }


    @MethodDeclare(
            label = "保存团队成员", how = "", what = "", why = "",
            inputs = {}
    )
    default void createOrUpdateMember(String userName, String fullName, String password, String roleName) throws Exception {
        if (StrUtil.hasBlank(roleName, userName)) throw new RuntimeException("角色名称/用户名不得为空");
        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(GroupChatConstants.BusDomainCode);
        if (observer == null) throw new RuntimeException("无法找到对应的业务域");

        UserRole userRole = UserRole.fromRoleName(roleName);
        if (userRole == null) throw new RuntimeException(StrUtil.format("未知的用户角色[{}]", roleName));


        try (IDao dao = IDaoService.newIDao()) {
            User user = IUserMgr.get().queryUserByName(dao, GroupChatConstants.UserModelId, userName);

            if (user == null) {
                if (StrUtil.isBlank(password)) throw new RuntimeException("密码不得为空");
                user = Op.getOrCreateUserQuickly(dao, observer,
                        GroupChatConstants.UserModelId, userName, password);
            }

            if (user == null) throw new RuntimeException(StrUtil.format("创建用户[{}]失败", userName));


            // 先移除所有角色
            unmountUserAllRole(dao, observer, GroupChatConstants.OrgModelId,
                    GroupChatConstants.UserModelId, user);

            Role targetRole = Op.getOrCreateSimpleRole(dao, observer, GroupChatConstants.OrgModelId,
                    userRole.rolePath, "成员");

            if (targetRole == null)
                throw new RuntimeException(StrUtil.format("无法找到角色[{}→{}]", userRole.rolePath, "成员"));


            user.setFullName(fullName);
            if (!StrUtil.isBlank(password)) {
                user.setPassword(new Password().setValue(password));

            }

            IUserMgr.get().updateUser(dao, user);

            Op.addUserToAssignRole(dao, GroupChatConstants.OrgModelId,
                    GroupChatConstants.UserModelId, targetRole, user.getUserName());

            dao.commit();

        }
    }


    @MethodDeclare(
            label = "获取房间列表", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(desc = "", label = "运行上下文", name = "rtx", exampleValue = "$IDCRuntimeContext$"),
            }
    )
    default List<JSONObject> getRoomInfoList(IDCRuntimeContext rtx) throws Exception {

        AppUserInfo userInfo = HttpSessionUtil.getSessionInfo(AppUserInfo.class);
        if (userInfo == null) throw new RuntimeException("用户信息不存在");

        String userCode = userInfo.getUserId();
        try (IDao dao = IDaoService.newIDao()) {
            List<Form> resultList = IGroupChatRoomService.get().getRoomInfoList(dao);
            if (Op.isEmpty(resultList)) return new ArrayList<>();

            boolean isAdmin = isAdminUser(dao, userCode);

            List<JSONObject> resultJsonList = new ArrayList<>();
            for (Form form : resultList) {
                if (isAdmin || canUserAccessRoom(form, userCode)) {
                    resultJsonList.add(FormToJsonConversionUtil.convert(form));
                }
            }
            return resultJsonList;
        }
    }


    @MethodDeclare(
            label = "获取房间信息", how = "", what = "", why = "",
            inputs = {
            }
    )
    default JSONObject getRoomInfo(String roomCode, Boolean isGetChatMsgs) throws Exception {
        AppUserInfo userInfo = HttpSessionUtil.getSessionInfo(AppUserInfo.class);
        if (userInfo == null) throw new RuntimeException("用户信息不存在");

        String userCode = userInfo.getUserId();

        try (IDao dao = IDaoService.newIDao()) {
            Form form = IGroupChatRoomService.get().getRoomInfo(dao, roomCode, isGetChatMsgs);
            if (form != null) {
                boolean isAdmin = isAdminUser(dao, userCode);
                if (isAdmin || canUserAccessRoom(form, userCode)) {
                    return FormToJsonConversionUtil.convert(form);
                }

            }
        }

        return null;

    }

    @MethodDeclare(
            label = "删除房间信息", how = "", what = "", why = "",
            inputs = {}
    )
    default void deleteRoomInfo(String busDomainCode) throws Exception {

        // FIXME 鉴权！
        try (IDao dao = IDaoService.newIDao()) {

            // 移除房间信息
            IGroupChatRoomService.get().deleteRoomInfoByBusDomainCode(dao, busDomainCode);

            // 其他要删除的丢到线程池
            CompletableFuture.runAsync(() -> {
                try {
                    IGroupChatMessageService.get().removeAllByBusDomainCode(dao, busDomainCode);
                    GroupChatEngineStoreUtil.removeStoredData(dao, busDomainCode);
                    IVotaForgeService.get().deleteBusDomain(busDomainCode);
                } catch (Exception e) {
                    ConsolePrintUtil.printRedLn(StrUtil.format("删除房间数据失败，业务域编号[{}]", busDomainCode));
                    Op.logException(e);
                }

            }, GroupChatThreadPollManager.get(ThreadPoolType.REGULAR_TASK));


            dao.commit();
        }


    }


    @MethodDeclare(
            label = "获取智能体列表", how = "", what = "", why = "",
            inputs = {}
    )
    default List<AgentDefinition> getAgentInfoList() throws Exception {

        // 1、获取默认群组的智能体列表
        GroupDefinition groupDefinition = GroupDefinitionManager
                .getDefinition(GroupChatConstants.DefaultGroupId);

        List<AgentDefinition> agentDefinitions = groupDefinition.getAgentDefinitions();
        if (Op.isEmpty(agentDefinitions)) throw new RuntimeException("系统内不存在任何智能体定义，请联系相关开发人员!");

        // 尝试更新最新的定义
        GroupDefinitionManager.tryUpdateDefinition(groupDefinition);

        return agentDefinitions;


    }


    // 保存智能体信息
    @MethodDeclare(
            label = "保存智能体信息", how = "", what = "", why = "",
            inputs = {}
    )
    default void saveAgentInfo(String agentId, String agentAvatar, String agentName, String agentBusinessRole, String agentDescription) throws Exception {

        try (IDao dao = IDaoService.newIDao()) {

            Form form = Op.queryFormByCondition(dao, AgentDefinition.FORM_MODEL_ID, "智能体ID", agentId, null);
            boolean isCreate = false;
            if (form == null) {

                isCreate = true;
                form = new Form(AgentDefinition.FORM_MODEL_ID)
                        .setAttrValue(Form.Code, IdUtil.fastSimpleUUID())
                        .setAttrValue("智能体ID", agentId);

            }

            form.setAttrValue("智能体名称", agentName);
            form.setAttrValue("智能体头像", agentAvatar);
            form.setAttrValue("智能体商业职务", agentBusinessRole);
            form.setAttrValue("智能体介绍", agentDescription);

            if (isCreate) {
                IFormMgr.get().createForm(dao, form);
            } else {
                IFormMgr.get().updateForm(dao, form);
            }

            dao.commit();


        }

    }


    @MethodDeclare(
            label = "保存系统设置", how = "", what = "", why = "",
            inputs = {}
    )
    default void saveSystemSettings(String configName, String effectRule, String configTable,
                                    Long createTime, Long updateTime, Boolean enabled) throws Exception {

        try (IDao dao = IDaoService.newIDao()) {

            Form form = Op.queryFormByCondition(dao, SystemSettingsDefinition.FORM_MODEL_ID,
                    "配置名称", configName, null);
            boolean isCreate = false;
            if (form == null) {
                isCreate = true;
                form = new Form(SystemSettingsDefinition.FORM_MODEL_ID)
                        .setAttrValue(Form.Code, IdUtil.fastSimpleUUID());
            }

            form.setAttrValue("配置名称", configName);
            form.setAttrValue("生效规则", effectRule);
            form.setAttrValue("配置表", configTable);
            form.setAttrValue("创建时间", createTime == null ? System.currentTimeMillis() : createTime);
            form.setAttrValue("修改时间", updateTime == null ? System.currentTimeMillis() : updateTime);
            form.setAttrValue("是否启用", enabled != null && enabled);

            if (isCreate) {
                IFormMgr.get().createForm(dao, form);
            } else {
                IFormMgr.get().updateForm(dao, form);
            }

            dao.commit();
        }
    }


    @MethodDeclare(
            label = "获取系统设置", how = "", what = "", why = "",
            inputs = {}
    )
    default SystemSettingsDefinition getSystemSettings(String configName) throws Exception {

        try (IDao dao = IDaoService.newIDao()) {
            Form form = Op.queryFormByCondition(dao, SystemSettingsDefinition.FORM_MODEL_ID,
                    "配置名称", configName, cnd -> {
                        cnd.where().andEquals(Op.getFieldCode("是否启用"), true);
                        return cnd;
                    }
            );

            if (form == null) return null;
            return SystemSettingsDefinition.fromForm(form);
        }
    }


    // ========================= 支撑方法 =========================


    // 判断指定用户是否为管理员
    default boolean isAdminUser(IDao dao, String userCode) throws Exception {
        if (StrUtil.isBlank(userCode)) return false;
        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(GroupChatConstants.BusDomainCode);
        if (observer == null) return false;
        Role adminRole = Op.getOrCreateSimpleRole(dao, observer, GroupChatConstants.OrgModelId,
                UserRole.ADMIN.rolePath, "成员");
        if (adminRole == null) return false;
        List<User> adminUsers = IRoleMgr.get().queryMountedUserList(dao, adminRole.getUuid(), GroupChatConstants.UserModelId);
        if (Op.isEmpty(adminUsers)) return false;
        for (User user : adminUsers) {
            if (userCode.equals(user.getCode())) return true;
        }
        return false;
    }


    // 判断用户是否有权访问某个房间（是创建者或房间成员之一）
    default boolean canUserAccessRoom(Form roomForm, String userCode) throws Exception {
        if (StrUtil.isBlank(userCode)) return false;
        if (isRoomCreator(roomForm, userCode)) return true;
        return isRoomMember(roomForm, userCode);
    }


    // 判断用户是否为房间创建者
    default boolean isRoomCreator(Form roomForm, String userCode) throws Exception {
        AssociationData creator = roomForm.getAssociation("创建者");
        if (creator == null) return false;
        return userCode.equals(creator.getValue());
    }


    // 判断用户是否在房间成员列表中
    default boolean isRoomMember(Form roomForm, String userCode) throws Exception {
        List<AssociationData> members = roomForm.getAssociations("房间成员");
        if (Op.isEmpty(members)) return false;
        for (AssociationData member : members) {
            if (userCode.equals(member.getValue())) return true;
        }
        return false;
    }


    // 移除用户所有角色
    default void unmountUserAllRole(IDao dao, OctoDomainOpObserver observer, String orgModeId, String userModelId, User user) throws Exception {
        if (StrUtil.hasBlank(orgModeId, userModelId))
            throw new RuntimeException("组织模型Id,用户模型I");
        if (user == null) throw new RuntimeException("用户不得为空");

        for (UserRole userRole : UserRole.values()) {

            Role targetRole = Op.getOrCreateSimpleRole(dao, observer, orgModeId,
                    userRole.rolePath, "成员");
            if (targetRole == null) continue;

            IRoleMgr.get().unmountRoleFromUser(dao, targetRole.getUuid(),
                    userModelId, CollUtil.newArrayList(user.getUuid()));


        }


    }


}
