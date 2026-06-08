package ai.agent.util.groupChat;

import ai.agent.constant.AppConstants;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.taskboard.TaskExecutionItemDto;
import ai.agent.dto.groupChat.taskboard.TaskUpdateDto;
import ai.agent.dto.groupChat.textStyle.ErrorItemDto;
import ai.agent.dto.groupChat.textStyle.InteractionDto;
import ai.agent.dto.groupChat.textStyle.OmittedTextDto;
import ai.agent.dto.groupChat.textStyle.TodoItemDto;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.enums.NotificationEnums;
import ai.agent.enums.TextPayloadStyle;
import ai.agent.util.ConsolePrintUtil;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ai.agent.constant.AppConstants.AGENT_ID_SYSTEM_DEFAULT;
import static ai.agent.enums.OperateMessageEnums.*;

@Comment("群聊引擎消息快速")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-08", updateTime = "2025-09-08"
)
public class GroupChatMessageSender {

    // 系统消息指令
    public static class System {

        // 打开全屏Loading
        public static void showFullScreenLoading(GroupChatEngine engine, String title) {
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("title", title);
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    SHOW_FULL_SCREEN_LOADING.toString(),
                    paramsMap,
                    null
            );
            engine.submitAgentMessage(op);
        }

        // 关闭全屏Loading
        public static void closeFullScreenLoading(GroupChatEngine engine) {
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    CLOSE_FULL_SCREEN_LOADING.toString(),
                    null,
                    null
            );
            engine.submitAgentMessage(op);
        }

    }

    // 工作区
    public static class WorkSpace {

        // 打开
        public static void open(GroupChatEngine engine) {
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    WORKSPACE_OPEN.toString(),
                    new HashMap<>(),
                    null
            );
            engine.submitAgentMessage(op);
        }

        // 关闭
        public static void close(GroupChatEngine engine) {
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    WORKSPACE_CLOSE.toString(),
                    new HashMap<>(),
                    null
            );
            engine.submitAgentMessage(op);
        }


    }

    // 画布
    public static class Canvas {

        // 打开
        public static void open(GroupChatEngine engine, String senderId, String canvasType, Map<String, Object> canvasParams) {
            open(engine, senderId, canvasType, canvasParams, null);
        }

        // 打开（支持子会话）
        public static void open(GroupChatEngine engine, String senderId, String canvasType, Map<String, Object> canvasParams, String subSessionId) {
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("canvasType", canvasType);
            paramsMap.put("canvasParams", canvasParams);

            // 如果是子会话，添加targetCanvasSessionId
            if (StrUtil.isNotBlank(subSessionId)) {
                paramsMap.put("targetCanvasSessionId", subSessionId);
                ConsolePrintUtil.printGreenLn(StrUtil.format("[子会话画布] 打开画布 - SessionId:{}, Type:{}, Params:{}",
                        subSessionId, canvasType, JSONUtil.toJsonStr(canvasParams)));
            } else {
                ConsolePrintUtil.printWhiteLn(StrUtil.format("paramsMap:{}", JSONUtil.toJsonStr(paramsMap)));
            }

            Message op = MessageBuilder.createOperateMessage(
                    senderId,
                    CANVAS_OPEN.toString(),
                    paramsMap,
                    null
            );
            engine.submitAgentMessage(op);
        }

        // 关闭
        public static void close(GroupChatEngine engine) {
            Map<String, Object> paramsMap = new HashMap<>();
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    CANVAS_CLOSE.toString(),
                    paramsMap,
                    null
            );
            engine.submitAgentMessage(op);
        }

        // 子画布-移到主画布
        public static void moveToPrimary(GroupChatEngine engine, String subSessionId) {
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("subSessionId", subSessionId);
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    SUB_CANVAS_MOVE_TO_PRIMARY.toString(),
                    paramsMap,
                    null
            );
            engine.submitAgentMessage(op);
        }
        // 子画布-移出主画布
        public static void moveOutPrimary(GroupChatEngine engine, String subSessionId) {
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("subSessionId", subSessionId);
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    SUB_CANVAS_MOVE_OUT_PRIMARY.toString(),
                    paramsMap,
                    null
            );
            engine.submitAgentMessage(op);
        }


        // 子画布-移到主画布
        public static void preview(GroupChatEngine engine, String subSessionId) {
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("subSessionId", subSessionId);
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    SUB_CANVAS_PREVIEW.toString(),
                    paramsMap,
                    null
            );
            engine.submitAgentMessage(op);
        }

        // 触发动作
        public static void callAction(GroupChatEngine engine, String requestId,
                                      String actionName,
                                      Map<String, Object> actionParams) {
            callAction(engine, requestId, actionName, actionParams, null);
        }

        // 触发动作（支持子会话）
        public static void callAction(GroupChatEngine engine, String requestId,
                                      String actionName,
                                      Map<String, Object> actionParams,
                                      String subSessionId) {
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("requestId", requestId);
            paramsMap.put("actionName", actionName);
            paramsMap.put("actionParams", actionParams);

            // 如果是子会话，添加targetCanvasSessionId
            if (StrUtil.isNotBlank(subSessionId)) {
                paramsMap.put("targetCanvasSessionId", subSessionId);
                ConsolePrintUtil.printGreenLn(StrUtil.format("[子会话画布] 调用动作 - SessionId:{}, Action:{}, Params:{}",
                        subSessionId, actionName, JSONUtil.toJsonStr(actionParams)));
            }

            Message op = MessageBuilder.createOperateMessage(
                    AppConstants.AGENT_ID_SYSTEM_SCHEDULER,
                    CANVAS_CALL_ACTION.toString(),
                    paramsMap,
                    null
            );
            engine.submitAgentMessage(op);
        }

    }

    // 聊天相关
    public static class Chat {
        // 对话开始
        public static void chatBegin(GroupChatEngine engine) {
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    CHAT_BEGIN.toString(),
                    null,
                    null
            );

            engine.submitAgentMessage(op);
        }

        // 对话结束
        public static void chatEnd(GroupChatEngine engine) {
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    CHAT_END.toString(),
                    null,
                    null
            );
            engine.submitAgentMessage(op);
        }

        // 聚合开始
        public static void aggregationBegin(GroupChatEngine engine, String agentId) {
            Message op = MessageBuilder.createOperateMessage(
                    AGENT_ID_SYSTEM_DEFAULT,
                    AGGREGATION_BEGIN.toString(),
                    null,
                    null
            );
            engine.submitAgentMessage(op);

        }

        // 聚合结束
        public static void aggregationEnd(GroupChatEngine engine, String agentId) {
            Message op = MessageBuilder.createOperateMessage(
                    agentId,
                    AGENT_ID_SYSTEM_DEFAULT,
                    null,
                    null
            );
            engine.submitAgentMessage(op);
        }


    }

    // 通知
    public static class Notice {

        // 弹出Toast提示
        public static void toast(GroupChatEngine engine, String agentId, NotificationEnums type, String message) {
            if (StrUtil.isBlank(agentId)) agentId = AGENT_ID_SYSTEM_DEFAULT;
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("type", type.toString());
            paramsMap.put("message", message);
            engine.submitAgentMessage(MessageBuilder.createOperateMessage(
                    agentId,
                    SHOW_TOAST.toString(),
                    paramsMap,
                    null
            ));
        }

        // 提交提示
        public static void hint(GroupChatEngine engine, String agentId, String text) {
            if (StrUtil.isBlank(agentId)) agentId = AGENT_ID_SYSTEM_DEFAULT;
            Message message = MessageBuilder.createAgentTextMessageWithStyle(
                    agentId,
                    text,
                    TextPayloadStyle.HINT.getValue()
            );
            engine.submitAgentMessage(message);

        }

        // 省略消息
        public static void omitted(GroupChatEngine engine, String agentId, OmittedTextDto omittedTextDto) {
            if (StrUtil.isBlank(agentId)) agentId = AGENT_ID_SYSTEM_DEFAULT;
            Message message = MessageBuilder.createAgentTextMessageWithStyle(
                    agentId,
                    JSONUtil.toJsonStr(omittedTextDto),
                    TextPayloadStyle.OMITTED.getValue()
            );
            engine.submitAgentMessage(message);
        }

    }

    // 样式文本
    public static class StyleText {

        // 展示待办事项
        public static void todo(GroupChatEngine engine, String agentId, List<TodoItemDto> todos) {
            if (StrUtil.isBlank(agentId)) agentId = AGENT_ID_SYSTEM_DEFAULT;
            Message message = MessageBuilder.createAgentTextMessageWithStyle(
                    agentId,
                    JSONUtil.toJsonStr(todos),
                    TextPayloadStyle.TODO.getValue()
            );
            engine.submitAgentMessage(message);

        }

        // 展示交互消息
        public static void interaction(GroupChatEngine engine, String agentId, InteractionDto dto) {
            if (StrUtil.isBlank(agentId)) agentId = AGENT_ID_SYSTEM_DEFAULT;
            Message message = MessageBuilder.createAgentTextMessageWithStyle(
                    agentId,
                    JSONUtil.toJsonStr(dto),
                    TextPayloadStyle.INTERACTIVE.getValue()
            );
            engine.submitAgentMessage(message);
        }

        // 展示错误信息
        public static void error(GroupChatEngine engine, String agentId, List<ErrorItemDto> errors) {
            if (StrUtil.isBlank(agentId)) agentId = AGENT_ID_SYSTEM_DEFAULT;
            Message message = MessageBuilder.createAgentTextMessageWithStyle(
                    agentId,
                    JSONUtil.toJsonStr(errors),
                    TextPayloadStyle.ERROR.getValue()
            );
            engine.submitAgentMessage(message);
        }


    }

    // 任务执行消息发送器
    public static class Task {

        // 创建新的任务执行消息
        public static Message create(GroupChatEngine engine, String agentId, List<TaskExecutionItemDto> tasks) {
            if (StrUtil.isBlank(agentId)) agentId = AGENT_ID_SYSTEM_DEFAULT;
            Message message = MessageBuilder.createAgentTextMessageWithStyle(
                    agentId,
                    JSONUtil.toJsonStr(tasks),
                    TextPayloadStyle.TASK_EXECUTION.getValue()
            );
            engine.submitAgentMessage(message);
            return message;
        }

        // 更新已存在的任务执行消息
        public static void update(GroupChatEngine engine, String agentId, String originalMsgId, List<TaskExecutionItemDto> tasks) {
            if (StrUtil.isBlank(agentId)) agentId = AGENT_ID_SYSTEM_DEFAULT;
            Message message = MessageBuilder.createAgentTextMessageWithStyle(
                    agentId,
                    JSONUtil.toJsonStr(tasks),
                    TextPayloadStyle.TASK_EXECUTION.getValue()
            );
            if (StrUtil.isNotBlank(originalMsgId)) {
                message.setRerenderMsgId(originalMsgId);
            }
            engine.submitAgentMessage(message);
        }

        // 增量更新单个任务项
        public static void updateItem(GroupChatEngine engine, String agentId, String originalMsgId, TaskExecutionItemDto task) {
            if (StrUtil.isBlank(agentId)) agentId = AGENT_ID_SYSTEM_DEFAULT;

            // 创建增量更新消息
            TaskUpdateDto updateDto = new TaskUpdateDto()
                    .setOperation(TaskUpdateDto.Operation.UPDATE)
                    .setTask(task);

            Message message = MessageBuilder.createAgentTextMessageWithStyle(
                    agentId,
                    JSONUtil.toJsonStr(updateDto),
                    TextPayloadStyle.TASK_EXECUTION.getValue()
            );
            if (StrUtil.isNotBlank(originalMsgId)) {
                message.setRerenderMsgId(originalMsgId);
            }
            engine.submitAgentMessage(message);
        }

        // 创建待执行的任务
        public static TaskExecutionItemDto newPending(String name, String description) {
            return new TaskExecutionItemDto()
                    .setId(IdUtil.fastSimpleUUID())
                    .setName(name)
                    .setDescription(description)
                    .setStatus(TaskExecutionItemDto.Status.PENDING);
        }
    }

    // 进度消息
    public static class Progress {

        // 展示进度视图
        public static void open(GroupChatEngine engine, String progressId,
                                String title) {

            Map<String, String> paramsMap = new HashMap<>();
            paramsMap.put("progressId", progressId);
            paramsMap.put("title", title);


            Message op = MessageBuilder.createOperateMessage(
                    AppConstants.AGENT_ID_SYSTEM_SCHEDULER,
                    POP_PROGRESS.toString(),
                    paramsMap,
                    null
            );

            engine.submitAgentMessage(op);
        }

        // 关闭进度视图
        public static void close(GroupChatEngine engine, String progressId) {
            Map<String, String> paramsMap = new HashMap<>();
            paramsMap.put("progressId", progressId);


            Message op = MessageBuilder.createOperateMessage(
                    AppConstants.AGENT_ID_SYSTEM_SCHEDULER,
                    CLOSE_PROGRESS.toString(),
                    paramsMap,
                    null
            );

            engine.submitAgentMessage(op);
        }

        // 添加进度消息
        public static void addMsg(GroupChatEngine engine, String progressId, String message) {
            Map<String, String> paramsMap = new HashMap<>();
            paramsMap.put("progressId", progressId);
            paramsMap.put("message", message);


            Message op = MessageBuilder.createOperateMessage(
                    AppConstants.AGENT_ID_SYSTEM_SCHEDULER,
                    ADD_PROGRESS_MESSAGE.toString(),
                    paramsMap,
                    null
            );

            engine.submitAgentMessage(op);
        }


        // 添加错误信息
        public static void addErrorMsg(GroupChatEngine engine, String progressId,
                                       String title) {

            Map<String, String> paramsMap = new HashMap<>();
            paramsMap.put("progressId", progressId);
            paramsMap.put("title", title);
            paramsMap.put("type", "error");

            Message op = MessageBuilder.createOperateMessage(
                    AppConstants.AGENT_ID_SYSTEM_SCHEDULER,
                    POP_PROGRESS.toString(),
                    paramsMap,
                    null
            );

            engine.submitAgentMessage(op);
        }

        // 弹出系统错误信息
        public static void finishWithError(GroupChatEngine engine, String title, String message) {
            String instanceId = engine.getGroupChatInstance().getInstanceId();
            ConsolePrintUtil.printRedLn(StrUtil.format("群聊实例[{}] {}",
                    instanceId, message));

            String progressId = IdUtil.fastSimpleUUID();
            GroupChatMessageSender.Progress.addErrorMsg(engine, progressId, title);
            GroupChatMessageSender.Progress.addMsg(engine, progressId, message);


            engine.stop();


        }

    }


}
