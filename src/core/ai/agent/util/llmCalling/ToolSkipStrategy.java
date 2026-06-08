package ai.agent.util.llmCalling;

import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.impl.ListGroupMembersTool;
import ai.agent.engine.groupChat.tool.impl.VotaForge.VotaForge_BasicDeliveryTool;
import ai.agent.engine.groupChat.tool.impl.VotaForge.VotaForge_RequirementDocParseTool;
import ai.agent.engine.groupChat.tool.impl.canvas.CallCanvasActionTool;
import ai.agent.engine.groupChat.tool.impl.canvas.ListCanvasActionTool;
import ai.agent.engine.groupChat.tool.impl.canvas.SeeCanvasOperationGuideTool;
import ai.agent.engine.groupChat.tool.impl.task.*;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.util.Set;
import java.util.function.Function;

@Comment("工具跳过策略")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-02", updateTime = "2025-12-02"
)
public class ToolSkipStrategy {

    // 当非画布时跳过
    private static final Set<Class> STRATEGY_ON_NONCANVAS_MODE = CollUtil.newHashSet(
            ListCanvasActionTool.class,
            CallCanvasActionTool.class,
            SeeCanvasOperationGuideTool.class
    );

    // 当画布时跳过
    private static final Set<Class> STRATEGY_ON_CANVAS_MODE = CollUtil.newHashSet(
            VotaForge_BasicDeliveryTool.class,
            VotaForge_RequirementDocParseTool.class,
            MyTasksTool.class,
            CreateTaskTool.class,
            UpdateTaskStatusTool.class,
            BatchUpdateTaskStatusTool.class,
            CompleteTaskTool.class,
            ListTasksTool.class,
            ListGroupMembersTool.class
    );

    // 子会话中跳过的工具（禁止嵌套）
    private static final Set<Class> STRATEGY_ON_SUBSESSION_MODE = CollUtil.newHashSet(
            ListTasksTool.class,
            CompleteTaskTool.class,
            MyTasksTool.class,
            CreateTaskTool.class,
            UpdateTaskStatusTool.class,
            BatchUpdateTaskStatusTool.class,
            ListGroupMembersTool.class
    );



    public static final Function<Tool, Boolean> onCanvasMode = tool ->
            STRATEGY_ON_CANVAS_MODE.contains(tool.getClass());

    public static final Function<Tool, Boolean> onNonCanvasMode = tool ->
            STRATEGY_ON_NONCANVAS_MODE.contains(tool.getClass());

    public static final Function<Tool, Boolean> onSubSessionMode = tool ->
            STRATEGY_ON_SUBSESSION_MODE.contains(tool.getClass());



};
