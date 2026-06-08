package ai.agent.service.frontendCalling;

import ai.agent.dto.frontendCalling.FrontendActionDto;
import ai.agent.dto.groupChat.canvas.CanvasStatus;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.enums.CanvasStatusEnums;
import ai.agent.util.ConsolePrintUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 前端动作管理类
 * 注意：Canvas切换的时候会销毁，只保留一次
 */
public class FrontendCanvasManager implements Serializable {


    public static final String DEFAULT_OPERATION_GUIDANCE = "暂无使用手册";
    public static final String CANVAS_ACTION_GET_CANVAS_STATUS = "get_canvas_status";
    private static final int VERSION_MOD = 100_000; // 版本号取模值，防止溢出

    // Canvas 使用手册
    private CanvasStatus currentCanvasStatus;
    private String operationGuidance = DEFAULT_OPERATION_GUIDANCE;
    // Canvas 提供的动作集合
    private final Map<String, FrontendActionDto> actionMap;
    // 画布动作版本号，每次注册时递增
    private final AtomicInteger canvasActionVersion;

    public FrontendCanvasManager() {
        currentCanvasStatus = null;
        actionMap = new ConcurrentHashMap<>();
        canvasActionVersion = new AtomicInteger(0);
    }


    // 注册动作
    // isClear: 是否清理已存在的动作
    public void register(CanvasStatus currentCanvasStatus,
                         String operationGuidance, List<FrontendActionDto> actions, boolean isClear) {
        try {
            if (isClear) actionMap.clear();
            this.currentCanvasStatus = currentCanvasStatus;
            this.operationGuidance = StrUtil.isNotBlank(operationGuidance) ? operationGuidance :
                    DEFAULT_OPERATION_GUIDANCE;

            if (CollUtil.isNotEmpty(actions)) {
                for (FrontendActionDto action : actions) {
                    registerAction(action);
                }
            }

            // 版本号递增（取模防止溢出）
            canvasActionVersion.set((canvasActionVersion.get() + 1) % VERSION_MOD);
            ConsolePrintUtil.printGreenLn(StrUtil.format(
                    "[画布动作注册] 版本号更新: {}, 当前动作数量: {}",
                    canvasActionVersion.get(), actionMap.size()
            ));

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn("FrontendCanvasManager.register:\n" + ExceptionUtils.getFullStackTrace(e));
        }

    }

    public CanvasStatus getCurrentCanvasStatus() {
        return currentCanvasStatus;
    }

    // 获取动作
    public FrontendActionDto getAction(String actionName) {
        return actionMap.get(actionName);
    }

    // 获取操作指导
    public String getOperationGuidance() {
        return operationGuidance;
    }

    // 获取动作（按动作名称排序，确保顺序稳定，利于KV缓存）
    public List<FrontendActionDto> listAction() {
        List<FrontendActionDto> list = new ArrayList<>(actionMap.values());
        list.sort(java.util.Comparator.comparing(FrontendActionDto::getActionName));
        return list;
    }

    // String格式的ListAction（使用排序后的列表，确保顺序稳定）
    public String listActionStr() {
        if (actionMap.isEmpty()) return "当前Canvas没有注册任何动作";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (FrontendActionDto action : listAction()) {
            sb.append(StrUtil.format("\n#### {}、动作名称: {}\n",
                    ++i, action.getActionName()));
            sb.append("动作介绍: ").append(action.getActionDescription()).append("\n");
            sb.append("入参介绍: ").append(action.getActionParameterSchema()).append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }


    // 检查是否打开了Canvas
    public boolean isCanvasOpen(GroupChatEngine chatEngine) {
        try {
            // 检查是不是子会话里
            boolean isSubSession = chatEngine.getGroupChatInstance() instanceof SubSessionInstance;
            String subSessionId = !isSubSession ? null : chatEngine.getGroupChatInstance().getInstanceId();

            Object result = FrontendAsyncCallService
                    .callAction(chatEngine,
                            CANVAS_ACTION_GET_CANVAS_STATUS, null, 10, subSessionId);
            ConsolePrintUtil.printYellowLn(
                    StrUtil.format(
                            "正在获取当前画布状态, 获取结果为[{}]，实际画布状态:[{}]",
                            JSONUtil.toJsonStr(result),
                            JSONUtil.toJsonStr(getCurrentCanvasStatus())
                    )
            );

            boolean isOpen = result instanceof String && result.equals(CanvasStatusEnums.OPENED.toString());
            ConsolePrintUtil.printYellowLn(
                    StrUtil.format(
                            "当前画布状态为[{}]",
                            isOpen ? "已打开" : "已关闭"
                    )
            );
            return isOpen;

        } catch (Exception e) {
            return false;
        }


    }


    // ========================= 支撑方法 =========================

    // 获取当前版本号
    public int getCanvasActionVersion() {
        return canvasActionVersion.get();
    }

    // ========================= 支撑方法 =========================

    // 注册动作
    private void registerAction(FrontendActionDto frontendActionDto) {
        String actionName = frontendActionDto.getActionName();
        actionMap.put(actionName, frontendActionDto);
    }

}
