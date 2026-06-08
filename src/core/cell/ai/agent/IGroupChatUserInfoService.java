package cell.ai.agent;


import ai.agent.constant.AppConstants;
import ai.agent.dto.groupChat.UserInfoDto;
import bap.cells.Cells;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import org.nutz.dao.entity.annotation.Comment;
import panelx.dto.agentforge.AgentForgeSettingDto;
import panelx.dto.agentforge.AgentForgeUserInfoDto;
import panelx.utils.AgentForgeJWTUtil;

import static ai.agent.constant.GroupChatConstants.FormModelId_GroupChatUserInfo;

@Comment("群聊用户权限服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-11", updateTime = "2025-09-11"
)
// cell.ai.agent.IGroupChatUserInfoService
public interface IGroupChatUserInfoService extends IGroupChatBasicService {

    static IGroupChatUserInfoService get() {
        return Cells.get(IGroupChatUserInfoService.class);
    }


    // 检查并生效令牌
    default void checkAndTakeEffectToken(String token, String sourceAppUrl) {
       if(!AppConstants.enableAuthLogic) return;

        AgentForgeSettingDto agentForgeSettingDto = null;
        try {

            agentForgeSettingDto = AgentForgeJWTUtil.parseTokenToDTO(token,
                    AppConstants.PRIVATE_KEY_USER_INFO_JWT);

        } catch (Exception e) {
            Op.logException(e);
            throw new RuntimeException("令牌非法，请联系平台方处理！");
        }

        String url = agentForgeSettingDto.getUrl();
        if (StrUtil.isNotBlank(url) && !url.equals(sourceAppUrl)) {
//            throw new RuntimeException(
//                    StrUtil.format("令牌绑定的地址为[{}]，而你的服务地址为[{}]", url, sourceAppUrl)
//            );
        }

        Long expireTime = agentForgeSettingDto.getExpireTime();
        if (expireTime != null && System.currentTimeMillis() >= expireTime) {
            throw new RuntimeException("当前令牌已过期，请联系平台方处理！");
        }

        int maxGPTChatLimit = agentForgeSettingDto.getMaxGPTChatLimit();
        if (maxGPTChatLimit <= 0) {
            throw new RuntimeException("平台并未授权给你智能体回复消息的权限，请联系平台方处理！");
        }

        AgentForgeUserInfoDto user = agentForgeSettingDto.getUser();


        UserInfoDto userInfoDto = new UserInfoDto()
                .setUserName(user.getUserName())
                .setPhone(user.getPhone())
                .setEnterpriseName(user.getEnterpriseName())
                .setCurrentModelCallingNo(0L)
                .setMaxModelCallingNo((long) maxGPTChatLimit)
                .setOriginalToken(token)
                ;

        takeEffectToken(userInfoDto, token);


    }

    // 生效令牌
    default void takeEffectToken(UserInfoDto userInfoDto, String token) {
        if (userInfoDto == null) return;
        try (IDao dao = IDaoService.newIDao()) {
            ResultSet<Form> userInfoRs = IFormMgr.get().queryFormPage(dao, FormModelId_GroupChatUserInfo,
                    null, 1, 1, false, false);

            if (!userInfoRs.isEmpty()) {
                Form existedUserInfoForm = userInfoRs.getDataList().get(0);
                UserInfoDto existedUserInfoDto = UserInfoDto.newDto(existedUserInfoForm);

                // 如果还是库里面的令牌就不需要生效
                if (token.equals(existedUserInfoDto.getOriginalToken())) {
                    return;
                }

                // 如果是最新的令牌就删除
                IFormMgr.get().deleteForm(dao, existedUserInfoForm.getFormModelId(), existedUserInfoForm.getUuid());

            }

            // 生效现在这个令牌
            Form userInfoForm = userInfoDto.toForm();

            IFormMgr.get().createForm(dao, userInfoForm);

            dao.commit();

        } catch (Exception e) {
            Op.logException(e);
            throw new RuntimeException(e);
        }
    }

    // 获取当前剩余调用次数
    default long getCurrentLeftModelCallingNo() {

        UserInfoDto userInfoDto = getCurrentUserInfo();
        if (userInfoDto == null)
            throw new RuntimeException("系统内不存在当前用户的信息");

        Long currentModelCallingNo = userInfoDto.getCurrentModelCallingNo();
        Long maxModelCallingNo = userInfoDto.getMaxModelCallingNo();

        return maxModelCallingNo - currentModelCallingNo;

    }

    // 增加模型调用次数
    default void increaseModelCallingNo() {

        // 当前的设计，单套GPF只服务于一个用户，因此无需进行过滤其他业务域数据
        try (IDao dao = IDaoService.newIDao()) {

            ResultSet<Form> userInfoRs = IFormMgr.get().queryFormPage(dao, FormModelId_GroupChatUserInfo,
                    null, 1, 1, true, true);

            if (userInfoRs.isEmpty())
                throw new RuntimeException("系统内不存在当前用户的信息");

            Form userInfoForm = userInfoRs.getDataList().get(0);
            Long currentModelCallingNo = userInfoForm.getLong(UserInfoDto.FIELD_NAME_CURRENT_MODE_CALLING_NO);
            if (currentModelCallingNo == null) currentModelCallingNo = 0L;

            userInfoForm.setAttrValue(
                    UserInfoDto.FIELD_NAME_CURRENT_MODE_CALLING_NO,
                    currentModelCallingNo + 1
            );


            IFormMgr.get().updateForm(dao, userInfoForm);

            dao.commit();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    // 获取当前系统用户信息
    default UserInfoDto getCurrentUserInfo() {
        // 当前的设计，单套GPF只服务于一个用户，因此无需进行过滤其他业务域数据
        try (IDao dao = IDaoService.newIDao()) {

            ResultSet<Form> userInfoRs = IFormMgr.get().queryFormPage(dao, FormModelId_GroupChatUserInfo,
                    null, 1, 1, true, true);

            UserInfoDto userInfoDto = null;
            if (userInfoRs.isEmpty() ||
                    (userInfoDto = UserInfoDto.newDto(userInfoRs.getDataList().get(0))) == null)
                throw new RuntimeException("系统内不存在当前用户的信息");

            return userInfoDto;

        } catch (Exception e) {
            Op.logException(e);
            throw new RuntimeException(e);
        }
    }


}
