package ai.agent.dto.groupChat;

import ai.agent.constant.GroupChatConstants;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import org.nutz.dao.entity.annotation.Comment;


@Comment("用户信息Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-09", updateTime = "2025-09-09"
)
// FIXME 11-04：在上一个版本里一个系统只允许一个用户进行使用，但后续又增加了“团队成员”的概念，因此本处的UserInfo并非指代“团队成员”
// FIXME 即，这里的UserInfo泛指整个系统的注册人，或创建者的信息，是系统级别的
public class UserInfoDto {

    public static final String FORM_MODEL_ID = GroupChatConstants.FormModelId_GroupChatUserInfo;
    public static final String FIELD_NAME_ORIGINAL_TOKEN = "原始令牌";
    public static final String FIELD_NAME_MAX_MODE_CALLING_NO = "最大模型调用数";
    public static final String FIELD_NAME_CURRENT_MODE_CALLING_NO = "当前模型调用数";
    public static final String FIELD_NAME_USER_NAME = "用户名称";
    public static final String FIELD_NAME_PHONE = "手机号";
    public static final String FIELD_NAME_ENTERPRISE_NAME = "企业名称";

    private String userName;
    private String phone;
    private String enterpriseName;
    private Long currentModelCallingNo;
    private Long maxModelCallingNo;
    private String originalToken;


    // 从Form中转换
    public static UserInfoDto newDto(Form userInfoForm) {
        if (userInfoForm == null) return null;

        try {
            String userName = userInfoForm.getString(FIELD_NAME_USER_NAME);
            String phone = userInfoForm.getString(FIELD_NAME_PHONE);
            String enterpriseName = userInfoForm.getString(FIELD_NAME_ENTERPRISE_NAME);
            Long currentModelCallingNo = userInfoForm.getLong(FIELD_NAME_CURRENT_MODE_CALLING_NO);
            Long maxModelCallingNo = userInfoForm.getLong(FIELD_NAME_MAX_MODE_CALLING_NO);
            String originalToken = userInfoForm.getString(FIELD_NAME_ORIGINAL_TOKEN);

            if (StrUtil.isBlank(userName)) userName = "未知用户";
            if (currentModelCallingNo == null) currentModelCallingNo = 0L;
            if (maxModelCallingNo == null) maxModelCallingNo = 0L;

            return new UserInfoDto()
                    .setUserName(userName)
                    .setPhone(phone)
                    .setEnterpriseName(enterpriseName)
                    .setCurrentModelCallingNo(currentModelCallingNo)
                    .setMaxModelCallingNo(maxModelCallingNo)
                    .setOriginalToken(originalToken)
                    ;

        } catch (Exception e) {
            return null;
        }

    }


    // 转换为Form
    public Form toForm() {
        try {
            String uuid = IdUtil.fastUUID();
            Form form = new Form(FORM_MODEL_ID);
            form.setUuid(uuid).setAttrValue(Form.Code, uuid);
            form.setAttrValue(FIELD_NAME_USER_NAME, this.getUserName());
            form.setAttrValue(FIELD_NAME_PHONE, this.getPhone());
            form.setAttrValue(FIELD_NAME_ENTERPRISE_NAME, this.getEnterpriseName());
            form.setAttrValue(FIELD_NAME_CURRENT_MODE_CALLING_NO, this.getCurrentModelCallingNo());
            form.setAttrValue(FIELD_NAME_MAX_MODE_CALLING_NO, this.getMaxModelCallingNo());
            form.setAttrValue(FIELD_NAME_ORIGINAL_TOKEN, this.getOriginalToken());

            return form;
        } catch (Exception e) {
            return null;
        }


    }


    public String getPhone() {
        return phone;
    }

    public UserInfoDto setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getEnterpriseName() {
        return enterpriseName;
    }

    public UserInfoDto setEnterpriseName(String enterpriseName) {
        this.enterpriseName = enterpriseName;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public UserInfoDto setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public Long getCurrentModelCallingNo() {
        return currentModelCallingNo;
    }

    public UserInfoDto setCurrentModelCallingNo(Long currentModelCallingNo) {
        this.currentModelCallingNo = currentModelCallingNo;
        return this;
    }

    public Long getMaxModelCallingNo() {
        return maxModelCallingNo;
    }

    public UserInfoDto setMaxModelCallingNo(Long maxModelCallingNo) {
        this.maxModelCallingNo = maxModelCallingNo;
        return this;
    }

    public String getOriginalToken() {
        return originalToken;
    }

    public UserInfoDto setOriginalToken(String originalToken) {
        this.originalToken = originalToken;
        return this;
    }
}
