package ai.agent.engine.groupChat.tool.impl.VotaForge;

import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.ExecutionTraceDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.message.payload.AttachmentPayload;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.enums.GCEngineWorkCacheKey;
import ai.agent.enums.NotificationEnums;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cell.ai.agent.IVotaForgeService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Map;

/**
 * VotaForge_需求文档解析
 * 输入：最近一次附件消息的编号/名称（这里mock）
 * 输出：结构化需求数据（mock JSON）
 */
public class VotaForge_RequirementDocParseTool implements Tool {
    @Override
    public String getName() {
        return "VotaForge_RequirementDocParseTool";
    }

    @Override
    public String getCnName() {
        return "VotaForge_需求文档解析";
    }

    @Override
    public String getDescription() {
        return "对用户上传的需求文档进行结构化解析。工具会从历史中自动查找最近的附件消息, 不需要在聊天记录中寻找对应的文档。";
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {
        GroupChatToolContextAdapter gcCtx = convertToGroupChatToolContext(ctx);
        String currentAgentId = gcCtx.getCurrentAgentId();
        String groupInstanceId = gcCtx.getGroupInstanceId();
        GroupChatEngine chatEngine = gcCtx.getChatEngine();

        try {
            // 1、上传到VotaForge
            List<String> docCodes = doUploadDocumentToVotaForge(chatEngine, groupInstanceId);

            // 添加执行信息
            gcCtx.addStepExecutionTextMessage("文档已上传，准备解析");

            GroupChatMessageSender.Notice.toast(chatEngine, currentAgentId, NotificationEnums.SUCCESS,
                    "文档已上传，准备解析");

            // 缓存里先存一下，可能后面会用
            chatEngine.putEngineWorkCache(GCEngineWorkCacheKey.LAST_IMPORTED_DOCUMENT_CODES, docCodes);

            // 2、下发解析任务
            IVotaForgeService VotaForgeService = IVotaForgeService.get();

            List<String> processTaskIds = VotaForgeService
                    .issuedParseDocumentTask(groupInstanceId, docCodes);

            gcCtx.addExecutionTrace(new ExecutionTraceDto().setCategory("下发").setOperation("解析任务"));


            String progressId = IdUtil.fastSimpleUUID();
            GroupChatMessageSender.Progress.open(chatEngine, progressId, "解析文档中");
            GroupChatMessageSender.Progress.addMsg(chatEngine, progressId, StrUtil.format("解析任务已下发到VotaForge，任务实例编号:{}", processTaskIds));


            try {


                long startTimeVal = System.currentTimeMillis();
                long maxWaitTimeVal = 10 * 60 * 1000 + startTimeVal;
                // 最大时间暂定为10分钟
                // 后续前端加入【静默状态 + 通知】
                while (System.currentTimeMillis() < maxWaitTimeVal) {

                    GroupChatMessageSender.Progress.addMsg(chatEngine, progressId, "让我看下现在的任务状态");

                    boolean isDone = VotaForgeService.isFinishParseDocumentTaskByNodeChecking(processTaskIds);
                    if (isDone) break;


                    Thread.sleep(1000);

                    GroupChatMessageSender.Progress.addMsg(chatEngine, progressId, "当前还没有处理完毕，我准备等待10秒再来查看");


                    Thread.sleep(10 * 1000);

                    GroupChatMessageSender.Progress.addMsg(chatEngine, progressId, "正在快速构建蓝图... \n解析后请点击 [预览意图] 查漏补缺。若发现细节遗漏，直接将原文片段发给我，即可定向完善。");

                    Thread.sleep(2000);

                    GroupChatMessageSender.Progress.addMsg(chatEngine, progressId, "");

                    Thread.sleep(8000);


                }


                gcCtx.addExecutionTrace(new ExecutionTraceDto().setCategory("状态").setOperation("解析成功"));

                gcCtx.addStepExecutionTextMessage("[解析任务执行成功] 解析任务执行完成! 让我们进行发布");
                GroupChatMessageSender.Progress.addMsg(chatEngine, progressId, "解析任务执行完成! 让我们进行发布");


                // 3、解析任务运行完成，进行发布
                VotaForgeService.publishDocumentParsedResult(groupInstanceId, processTaskIds);

                gcCtx.addStepExecutionTextMessage("[解析任务执行成功] 解析任务的结果已发布成功");
                GroupChatMessageSender.Progress.addMsg(chatEngine, progressId, "解析任务的结果已发布成功");

                Thread.sleep(1000);


            } finally {
                GroupChatMessageSender.Progress.close(chatEngine, progressId);

            }


            return RespondDto.newStrSuccess("[解析任务执行成功]解析任务的结果已发布成功！", "");


        } catch (Exception e) {
            // 改为抛出异常，让上层统一错误处理与重试
            throw new RuntimeException("VotaForge_需求文档解析执行失败: " + e.getMessage(), e);
        }


    }


    // 上传文件发送到VotaForge
    public List<String> doUploadDocumentToVotaForge(GroupChatEngine chatEngine, String groupInstanceId) {
        AttachmentPayload attachmentPayload = null;
        for (Message message : chatEngine.getMessageHistoryManager().getFullHistory()) {
            if (message.isAttachmentMessage()) {
                attachmentPayload = message.getAttachmentPayload();
            }
        }

        if (attachmentPayload == null) throw new RuntimeException("群聊中未发现任何附件");


        List<String> documentCodes = null;
        try {
            String fileCode = attachmentPayload.getFileCode();
            documentCodes = IVotaForgeService.get().importDocument(groupInstanceId, fileCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return documentCodes;
    }
}

