package ai.agent.engine.graph.node.reflection;

import ai.agent.dto.graph.NodeExecutionResult;
import ai.agent.dto.groupChat.AgentExperienceDto;
import ai.agent.dto.groupChat.AgentExperienceItemDto;
import ai.agent.dto.groupChat.textStyle.OmittedTextDto;
import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.engine.graph.AbstractGraphNode;
import ai.agent.engine.graph.GraphContext;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cell.ai.agent.IAgentExperienceService;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;

import static ai.agent.constant.GraphConstants.NODE_CURATOR;

@Comment("策展节点")
@ClassDeclare(
        label = "Curator",
        what = "维护Playbook并聚合反思内容", why = "持续优化智能体行为准则", how = "LLM合并新旧规则",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-11", updateTime = "2025-12-11"
)
public class CuratorNode extends AbstractGraphNode {

    @Override
    public String getName() {
        return NODE_CURATOR;
    }

    @Override
    public NodeExecutionResult execute(GraphContext ctx) throws Exception {

        AgentInstance agentInstance = ctx.getAgentInstance();
        ReflectorNode.ReflectionResult reflectionResult = ReflectorNode.getReflectionResult(ctx);

        if (agentInstance == null) {
            throw new RuntimeException("智能体实例不得为空");
        }

        if (reflectionResult == null || StrUtil.isBlank(reflectionResult.getInsight())) {
            ConsolePrintUtil.printYellowLn("[策展节点] 没有反思结果，跳过策展");
            return NodeExecutionResult.terminate()
                    .withReason("无反思结果");
        }

        List<ReflectorNode.AgentReflection> agentReflections = reflectionResult.getAgentReflections();
        if (CollUtil.isEmpty(agentReflections)) {
            ConsolePrintUtil.printYellowLn("[策展节点] 没有智能体反思内容，跳过策展");
            return NodeExecutionResult.terminate()
                    .withReason("无智能体反思内容");
        }

        try (IDao dao = IDaoService.newIDao()) {
            for (ReflectorNode.AgentReflection agentReflection : agentReflections) {
                String agentId = agentReflection.getAgentId();
                if (StrUtil.isBlank(agentId)) continue;

                curateForAgent(ctx, dao, agentInstance, reflectionResult, agentReflection);
            }
            dao.commit();

            return NodeExecutionResult.terminate()
                    .withReason("策展完成")
                    .withSummary("已更新智能体Playbook");

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[策展节点] 执行异常: {}", ExceptionUtils.getFullStackTrace(e)));
            return NodeExecutionResult.error("策展节点执行异常: " + e.getMessage());
        }
    }

    // 为单个智能体执行策展
    private void curateForAgent(GraphContext ctx, IDao dao, AgentInstance agentInstance,
                                ReflectorNode.ReflectionResult reflectionResult,
                                ReflectorNode.AgentReflection agentReflection) throws Exception {

        String agentId = agentReflection.getAgentId();
        String agentName = agentInstance.getDefinition().getAgentName();

        ConsolePrintUtil.printCyanLn(StrUtil.format("[策展节点] 开始为智能体 {} 策展", agentId));

        String currentPlaybook = queryPlaybook(dao, agentId);

        List<LlmMessage> messages = buildCuratorPrompt(reflectionResult, agentReflection, currentPlaybook);

        String llmResponse = agentInstance.getLlmClient().callChat(
                agentInstance.getEffectiveLlmConfig(),
                messages
        );

        ConsolePrintUtil.printGreenLn(StrUtil.format("[策展节点] 智能体 {} LLM响应: {}", agentId, llmResponse));

        if (StrUtil.isNotBlank(llmResponse)) {
            List<AgentExperienceItemDto> newExperienceItems = parseExperienceItems(llmResponse);
            if (CollUtil.isNotEmpty(newExperienceItems)) {
                saveExperienceItems(dao, agentId, newExperienceItems);

                StringBuilder detailBuilder = new StringBuilder();
                for (int i = 0; i < newExperienceItems.size(); i++) {
                    AgentExperienceItemDto item = newExperienceItems.get(i);
                    detailBuilder.append(String.format("%d. %s", i + 1, item.getExperienceContent()));
                    if (i < newExperienceItems.size() - 1) {
                        detailBuilder.append("\n");
                    }
                }

                GroupChatMessageSender.Notice.omitted(ctx.getChatEngine(), agentId,
                        new OmittedTextDto()
                                .setTitle(StrUtil.format("{}从问题反馈中学习到了新的知识",
                                        agentName))
                                .setDetail(detailBuilder.toString())
                );
            }
        }
    }


    // ========================= 支撑方法 =========================

    // 构建策展Prompt（针对单个智能体）
    private List<LlmMessage> buildCuratorPrompt(ReflectorNode.ReflectionResult reflectionResult,
                                                ReflectorNode.AgentReflection agentReflection,
                                                String currentPlaybook) {
        
        String outputFormatExample = JSONUtil.toJsonPrettyStr(buildExampleOutput());
        
        String systemPrompt = StrUtil.format(
                "# Role\n" +
                        "你是一个规则策展人（Curator）。你的任务是维护智能体的经验库，将新的经验教训合并进去。\n\n" +

                        "# Current Experience\n" +
                        "```\n{}\n```\n\n" +

                        "# New Insight\n" +
                        "- 类别：{}\n" +
                        "- 总体经验：{}\n" +
                        "- 针对当前智能体的反思：{}\n\n" +

                        "# Task\n" +
                        "1. 分析新的经验教训是否与现有规则冲突\n" +
                        "2. 如果冲突，以新规则为准，更新旧规则\n" +
                        "3. 如果重复，忽略新规则\n" +
                        "4. 如果是新规则，添加到经验列表中\n" +
                        "5. 保持经验条目精简、清晰\n" +
                        "6. 每条经验都应该是可操作的、具体的指导\n\n" +

                        "# Output Format\n" +
                        "输出JSON格式的经验列表（包含之前版本的内容），每条经验包含内容和启用状态：\n" +
                        "```json\n{}\n```\n\n" +

                        "# Output Constraint\n" +
                        "1. 仅输出JSON格式，不要输出任何解释或多余字符\n" +
                        "2. experienceContent应该是简洁明确的经验描述\n" +
                        "3. isEnabled默认为true，除非该经验已过时或被新经验替代\n" +
                        "4. 确保JSON格式正确，可以被解析",
                currentPlaybook,
                reflectionResult.getCategory(),
                reflectionResult.getInsight(),
                agentReflection.getContent(),
                outputFormatExample
        );

        ConsolePrintUtil.printRedLn("systemPrompt:\n" + systemPrompt);

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage(LlmMessage.Role_System, systemPrompt));
        messages.add(new LlmMessage(LlmMessage.Role_User, "请更新经验库。"));

        return messages;
    }

    // 构建输出示例
    private List<AgentExperienceItemDto> buildExampleOutput() {
        List<AgentExperienceItemDto> example = new ArrayList<>();
        example.add(new AgentExperienceItemDto()
                .setExperienceContent("经验描述1")
                .setEnabled(true));
        example.add(new AgentExperienceItemDto()
                .setExperienceContent("经验描述2")
                .setEnabled(true));
        return example;
    }

    // 解析经验项列表
    private List<AgentExperienceItemDto> parseExperienceItems(String response) {
        try {
            String jsonStr = extractJson(response);
            if (StrUtil.isBlank(jsonStr)) {
                ConsolePrintUtil.printYellowLn("[策展节点] 无法提取JSON内容");
                return new ArrayList<>();
            }
            
            List<AgentExperienceItemDto> items = JSONUtil.toList(jsonStr, AgentExperienceItemDto.class);
            if (CollUtil.isEmpty(items)) {
                ConsolePrintUtil.printYellowLn("[策展节点] 解析后的经验列表为空");
                return new ArrayList<>();
            }
            
            return items;
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[策展节点] 解析经验列表失败: {}", ExceptionUtils.getFullStackTrace(e)));
            return new ArrayList<>();
        }
    }

    // 提取JSON字符串
    private String extractJson(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                String content = response.substring(start, end).trim();
                if (content.startsWith("json")) {
                    content = content.substring(4).trim();
                }
                return content;
            }
        }
        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }


    // ========================= 数据访问方法 =========================

    // 查询Playbook
    private String queryPlaybook(IDao dao, String agentId) throws Exception {
        AgentExperienceDto dto = IAgentExperienceService.get(true).queryByAgentId(dao, agentId);
        if (dto == null || StrUtil.isBlank(dto.convertExperienceItemsToText())) {
            return "null";
        }
        return dto.convertExperienceItemsToText();
    }

    // 保存经验项列表
    private void saveExperienceItems(IDao dao, String agentId, List<AgentExperienceItemDto> experienceItems) throws Exception {
        AgentExperienceDto dto = IAgentExperienceService.get(true).queryByAgentId(dao, agentId);
        if (dto == null) {
            dto = new AgentExperienceDto();
            dto.setAgentId(agentId);
            dto.setAgentName(agentId);
        }
        dto.setExperienceItems(experienceItems);
        IAgentExperienceService.get(true).save(dao, dto);

        ConsolePrintUtil.printGreenLn(StrUtil.format("========== 智能体[{}]最新经验库 ==========", agentId));
        ConsolePrintUtil.printWhiteLn(JSONUtil.toJsonPrettyStr(experienceItems));
        ConsolePrintUtil.printGreenLn("===================================");
    }

}
