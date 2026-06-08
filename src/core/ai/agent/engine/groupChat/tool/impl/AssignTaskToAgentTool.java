package ai.agent.engine.groupChat.tool.impl;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.constant.AppConstants;
import ai.agent.constant.SubSessionConstants;
import ai.agent.dto.groupChat.ExecutionTraceDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.engine.graph.GraphEngine;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.engine.groupChat.session.SubSessionMode;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@ToolDeclare(
        name = "AssignTaskToAgentTool",
        cnName = "指派任务给智能体",
        description = "将任务指派给群组中的指定智能体执行。支持同步（阻塞等待结果）和异步（立即返回）两种模式。",
        scope = ToolScope.GROUP_CHAT
)
public class AssignTaskToAgentTool extends AbsGroupChatTool {

    @ParamDeclare(description = "目标智能体ID，必须是群组中已存在的智能体", required = true, type = "string")
    private String agentId;

    @ParamDeclare(description = "任务名称，简单为要进行的任务起个名字", required = true, type = "string")
    private String taskName;
    @ParamDeclare(description = "任务描述，详细说明需要执行的任务内容", required = true, type = "string")
    private String taskDescription;
    @ParamDeclare(description = "执行模式：SYNC(同步阻塞等待结果) 或 ASYNC(异步立即返回)", required = false, type = "string", defaultValue = "SYNC")
    private String mode;

    @Override
    protected String executeInternal() {
        try {
            ConsolePrintUtil.printBlueLn("[AssignTaskToAgentTool] 开始执行 - agentId: " + agentId + ", mode: " + mode);

            if (StrUtil.isBlank(agentId)) throw new RuntimeException("agentId不能为空");
            if (StrUtil.isBlank(taskName)) throw new RuntimeException("taskName不能为空");
            if (StrUtil.isBlank(taskDescription)) throw new RuntimeException("taskDescription不能为空");

            GroupChatInstance groupChatInst = engine.getGroupChatInstance();
            AgentInstance targetAgent = groupChatInst.getAgent(agentId);
            if (targetAgent == null)
                throw new RuntimeException(StrUtil.format("未找到智能体:{}, 请检查agentId是否正确", agentId));


            AgentDefinition agentDefinition = targetAgent.getDefinition();

            // 添加执行轨迹
            context.addExecutionTrace(new ExecutionTraceDto().setCategory("任务")
                    .setOperation(StrUtil.format("[{}] → [{}]",
                            taskName, agentDefinition.getAgentName())));


            // 子会话模式
//            SubSessionMode sessionMode = SubSessionMode.fromString(mode);
            SubSessionMode sessionMode = SubSessionMode.ASYNC;

            // 创建子会话
            SubSessionInstance subSession = engine.getSubSessionManager().createSubSession(groupChatInst.getInstanceId(),
                    agentDefinition, taskName, sessionMode, engine);

            ConsolePrintUtil.printBlueLn("[AssignTaskToAgentTool] 创建子会话成功 - sessionId: " + subSession.getInstanceId());

            // 通知前端打开工作区
            GroupChatMessageSender.WorkSpace.open(engine);

            // 添加执行轨迹
            context.addExecutionTrace(new ExecutionTraceDto().setCategory("打开").setOperation("工作区"));


            if (sessionMode == SubSessionMode.ASYNC) {
                executeAsync(subSession, targetAgent);
                context.addExecutionTrace(new ExecutionTraceDto().setCategory("状态").setOperation("下发成功"));

                return buildAsyncResponse(subSession, agentDefinition);
            } else {
                String result = executeSync(subSession, targetAgent);
                context.addExecutionTrace(new ExecutionTraceDto().setCategory("状态").setOperation("执行完毕"));
                return result;
            }

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn("[AssignTaskToAgentTool] 执行异常: " + e.getMessage());
            return SubSessionConstants.ERROR_PREFIX + ExceptionUtils.getFullStackTrace(e);
        }
    }


    // 同步执行
    private String executeSync(SubSessionInstance subSession, AgentInstance targetAgent) {
        try {
            ConsolePrintUtil.printBlueLn("[AssignTaskToAgentTool] 启动同步子会话: " + subSession.getInstanceId());
            String result = doExecute(subSession, false);
            ConsolePrintUtil.printGreenLn("[AssignTaskToAgentTool] 同步执行完成");
            return result;
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn("[AssignTaskToAgentTool] 同步执行异常: " + e.getMessage());
            engine.getSubSessionManager().failSubSession(subSession.getInstanceId(), e.getMessage());
            return SubSessionConstants.ERROR_PREFIX + e.getMessage();
        }
    }

    // 异步执行
    private void executeAsync(SubSessionInstance subSession, AgentInstance targetAgent) {
        CompletableFuture.runAsync(() -> {
            try {
                ConsolePrintUtil.printBlueLn("[AssignTaskToAgentTool] [异步] 启动子会话: " + subSession.getInstanceId());
                doExecute(subSession, true);
                ConsolePrintUtil.printGreenLn("[AssignTaskToAgentTool] [异步] 执行完成");
            } catch (Exception e) {
                ConsolePrintUtil.printRedLn("[AssignTaskToAgentTool] [异步] 执行异常: " + e.getMessage());
                engine.getSubSessionManager().failSubSession(subSession.getInstanceId(), e.getMessage());
            }
        });
    }

    // 实际执行的核心方法
    private String doExecute(SubSessionInstance subSession, boolean isAsync) throws Exception {
        engine.getSubSessionManager().startSubSession(subSession.getInstanceId());

        Message triggerMessage = Message.createTextMessage(
                AppConstants.AGENT_ID_SYSTEM_SCHEDULER,
                taskDescription,
                null
        );

        GroupChatEngine subEngine = subSession.getGroupChatEngine();
        if (subEngine == null) {
            String errorMsg = "子会话引擎未初始化";
            if (isAsync) {
                ConsolePrintUtil.printRedLn("[AssignTaskToAgentTool] [异步] " + errorMsg);
            }
            throw new RuntimeException(errorMsg);
        }

        GraphEngine graphEngine = GraphEngine.Scene.newChat(triggerMessage);
        graphEngine.start(subEngine, subSession.getAgentInstance());

        String result = subSession.getTaskResult();
        if (result == null || result.isEmpty()) {
            result = extractLastAgentMessage(subSession);
        }

        synchronized (subSession) {
            subSession.markAsWaitingReply();
            subSession.setTaskResult(result != null ? result : "任务执行完成");
            subSession.notifyAll();
        }

        return result != null ? result : "任务执行完成";
    }

    private String buildAsyncResponse(SubSessionInstance subSession, AgentDefinition agentDef) {
        return String.format(
                "任务已指派给智能体【%s】(异步执行)\n" +
                        "任务ID: %s\n" +
                        "任务将在后台执行，你可以继续处理其他事情。\n" +
                        "执行完成后会通知你查看结果。",
                agentDef.getAgentName(),
                subSession.getInstanceId()
        );
    }

    private String extractLastAgentMessage(SubSessionInstance subSession) {
        try {
            AgentDefinition agentDef = subSession.getAgentDefinition();
            if (agentDef == null) return null;
            String agentId = agentDef.getAgentId();

            List<Message> messages = subSession.getMessageHistoryManager().getFullHistory();
            if (messages != null && !messages.isEmpty()) {
                for (int i = messages.size() - 1; i >= 0; i--) {
                    Message msg = messages.get(i);
                    if (agentId.equals(msg.getSenderId()) && msg.getTextPayload() != null) {
                        return msg.getTextPayload().getText();
                    }
                }
            }
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn("[AssignTaskToAgentTool] 提取消息异常: " + e.getMessage());
        }
        return null;
    }
}
