package cell.ai.agent.http;

import ai.agent.dto.groupChat.AgentChatRecordDto;
import ai.agent.dto.groupChat.UserInfoDto;
import ai.agent.dto.groupChat.admin.PlatformUsageSituationDto;
import ai.agent.dto.groupChat.admin.UserDocumentDto;
import bap.cells.BasicCell;
import cell.ai.agent.IGroupChatMessageService;
import cell.ai.agent.IGroupChatUserInfoService;
import cmn.anotation.ClassDeclare;
import cmn.http.servlet.mapping.RequestMappingContext;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.util.*;

@Comment("")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-16", updateTime = "2025-09-16"
)
public class CGroupChatAdminController extends BasicCell implements IGroupChatAdminController {


    RequestMappingContext context;

    @Override
    public RequestMappingContext getContext() {
        return context;
    }

    @Override
    public void setContext(RequestMappingContext context) {
        this.context = context;
    }

    @Override
    public PlatformUsageSituationDto getUsageSituation() throws Exception {

        UserInfoDto userInfoDto = IGroupChatUserInfoService
                .get().getCurrentUserInfo();

        List<AgentChatRecordDto> agentChatRecordDtos = IGroupChatMessageService
                .get().queryAll();

        PlatformUsageSituationDto usageSituationDto = new PlatformUsageSituationDto()
                .setUserName(userInfoDto.getUserName())
                .setEnterpriseName(userInfoDto.getEnterpriseName())
                .setPhone(userInfoDto.getPhone())
                .setCurrentModelCallingNo(userInfoDto.getCurrentModelCallingNo())
                .setMaxModelCallingNo(userInfoDto.getMaxModelCallingNo());

        // 用户上传的文件编号
        Set<UserDocumentDto> userDocumentFileCodes = new TreeSet<>(Comparator.comparing(UserDocumentDto::getDocCode));
        // 用户发送的信息
        List<String> userSentMessages = new ArrayList<>();
        // 文档解析次数
        Long documentAnalysesNo = 0L;
        // 上次活跃时间
        Long lastActiveTime = -1L;

        // 如果聊天记录不为空
        if (CollUtil.isNotEmpty(agentChatRecordDtos)) {

            for (AgentChatRecordDto agentChatRecordDto : agentChatRecordDtos) {
                String msgContent = agentChatRecordDto.getMsgContent();
                String msgAttachmentFileCode = agentChatRecordDto.getMsgAttachmentFileCode();
                Long createTime = agentChatRecordDto.getCreateTime();
                if (createTime != null && createTime > 0) {
                    lastActiveTime = Math.max(createTime, lastActiveTime);
                }


                if (StrUtil.isNotBlank(msgAttachmentFileCode)) {
                    userDocumentFileCodes.add(
                            new UserDocumentDto()
                                    .setDocCode(msgAttachmentFileCode)
                                    .setDocName(msgContent)
                    );
                    documentAnalysesNo++;

                    continue;
                }

                // 内容不为空 && 文件为空
                if (StrUtil.isNotBlank(msgContent) && StrUtil.isBlank(msgAttachmentFileCode)) {
                    userSentMessages.add(
                            StrUtil.format("发送时间:{}\n发送内容:{}", DateTime.of(lastActiveTime)
                                    .toString(DatePattern.NORM_DATETIME_FORMAT),msgContent)
                    );
                }

            }
        }

        usageSituationDto.setDocumentAnalysesNo(documentAnalysesNo)
                .setUserDocumentFile(new ArrayList<>(userDocumentFileCodes))
                .setUserSentMessages(userSentMessages)
                .setLastActiveTime(lastActiveTime);


        return usageSituationDto;
    }


}
