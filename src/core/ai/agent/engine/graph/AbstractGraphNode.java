package ai.agent.engine.graph;

import ai.agent.constant.GraphConstants;
import ai.agent.dto.groupChat.AgentExperienceDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.util.ConsolePrintUtil;
import cell.ai.agent.IAgentExperienceService;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.util.Collections;
import java.util.List;

@Comment("图-抽象节点")
@ClassDeclare(
        label = "",
        what = "提供节点通用工具方法", why = "避免子类重复实现", how = "抽象类提供辅助方法",
        developer = "裴硕", version = "2.0",
        createTime = "2025-12-01", updateTime = "2025-12-14"
)
public abstract class AbstractGraphNode implements GraphNode {

    private static final String PROCESS_DATA_KEY_PLAN_CONTENT = "$PLAN_CONTENT";

    // 获取触发消息
    public Message getTriggerMessage(GraphContext ctx) {
        return ctx.getBeginningData(GraphConstants.PARAM_TRIGGER_MESSAGE, Message.class);
    }


    // 获取消息窗口
    public List<LlmMessage> getWindowMessage(GraphContext ctx) {
        if (ctx == null) return Collections.emptyList();

        GroupChatEngine chatEngine = ctx.getChatEngine();
        if (chatEngine == null) return Collections.emptyList();

//        boolean isRunInSubSession = chatEngine.isRunInSubSession();
        boolean isRunInSubSession = false; // 暂时始终开启，后面加个总结？
        Boolean isStartAlwaysFreshContextMode = chatEngine.getEngineConfig().isStartAlwaysFreshContextMode();

        // 判定：看设置有没有开启全新上下文模式，如果开启了就不读取历史消息
        // 如果是子会话，暂不处理
        // FIXME 逻辑应该统一，子会话和主会话似乎现在没有必要使用纯净上下文了
        if (!isRunInSubSession && isStartAlwaysFreshContextMode) {
            ConsolePrintUtil.printCyanLn("设置[AlwaysFreshContextMode=TRUE]，只读取最新的用户消息");
            return Collections.emptyList();
        }
        return chatEngine.getMessageHistoryManager()
                .getWindowHistoryWithLlmMessage(true);

    }

    // 设置计划内容
    public void setPlan(GraphContext ctx, String planContent) {
        ctx.putProcessData(PROCESS_DATA_KEY_PLAN_CONTENT, planContent);
    }

    // 获取计划内容
    public String getPlan(GraphContext ctx) {
        return ctx.getProcessData(PROCESS_DATA_KEY_PLAN_CONTENT, String.class);
    }


    // 获取历史总结信息
    public String getHistorySummary(GraphContext ctx) {
        GroupChatEngine chatEngine = ctx.getChatEngine();
        GroupChatInstance instance = chatEngine.getGroupChatInstance();
        if (instance instanceof SubSessionInstance) {
            SubSessionInstance subSession = (SubSessionInstance) instance;
            String summaries = subSession.getAllSummariesForPrompt();
            if (StrUtil.isNotBlank(summaries)) {
                return summaries;
            }
        }
        return "无";

    }


    // 获取智能体经验
    // TODO 添加缓存机制
    public AgentExperienceDto getAgentExperience(GraphContext ctx) {
        if (ctx == null) return null;
        AgentInstance agentInstance = ctx.getAgentInstance();
        if (agentInstance == null) return null;
        try (IDao dao = IDaoService.newIDao()) {

            // 1、查询这个智能体对应的经验
            AgentExperienceDto agentExperienceDto = IAgentExperienceService.get(true).queryByAgentId(dao, agentInstance.getDefinition().getAgentId());
            if (agentExperienceDto == null) return null;

            // 2、查询全局经验
            AgentExperienceDto globalExperienceDto = IAgentExperienceService.get(true).queryGlobalExperience(dao);
            if (globalExperienceDto != null) {
                String globalExperienceContent = globalExperienceDto.convertExperienceItemsToText();
                if (StrUtil.isNotBlank(globalExperienceContent)) {
                    agentExperienceDto.setGlobalExperienceContent(
                            globalExperienceContent
                    );
                }
            }


            return agentExperienceDto;
        } catch (Exception e) {
            return null;
        }
    }


}
