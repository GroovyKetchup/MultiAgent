package ai.agent.engine.groupChat.tool.impl;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.constant.AppConstants;
import ai.agent.dto.groupChat.textStyle.InteractionDto;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cn.hutool.core.util.StrUtil;

@ToolDeclare(
    name = "CreateInteractionTool",
    cnName = "创建用户交互",
    description = "向用户发起简单交互请求，用户点击后自动回复。支持5种交互类型：\n" +
            "1. options(选项列表) - 让用户从多个选项中选择一个，需提供options列表\n" +
            "   示例：{type:'options', prompt:'请选择数据源', options:[{label:'MySQL'},{label:'Oracle'}]}\n" +
//            "2. confirm(确认对话框) - 让用户确认或取消操作，需提供content和options(通常是'确定'/'取消')\n" +
//            "   示例：{type:'confirm', prompt:'删除确认', content:'确定要删除吗？', options:[{label:'确定'},{label:'取消'}]}\n" +
            "3. rating(评分) - 让用户评分，前端自动提供1-5星组件，无需提供options\n" +
            "   示例：{type:'rating', prompt:'请为本次服务评分'}\n" +
//            "4. yes_no(是否选择) - 简单的是/否问题，需提供options(['是','否'])\n" +
//            "   示例：{type:'yes_no', prompt:'是否继续？', options:[{label:'是'},{label:'否'}]}\n" +
            "用户点击后，前端会根据responseTemplate生成回复消息，默认模板为'选择了${option}'，其中${option}会被替换为用户选择的选项label。" +
            "AI可以通过设置responseTemplate自定义响应格式",
    scope = ToolScope.GROUP_CHAT
)
public class CreateInteractionTool extends AbsGroupChatTool {
    
    @ParamDeclare(
        description = "交互配置对象，包含以下字段：\n" +
                "- type(必需): 交互类型，可选值：options//rating" +
//                "/confirm/yes_no\n" +
                "- prompt(必需): 提示文本，向用户说明需要做什么\n" +
                "- content(可选): 详细内容，主要用于confirm类型的详细说明\n" +
                "- options(部分必需): 选项列表，每个选项包含label字段(显示文本)。options" +
//                "/confirm/yes_no" +
                "类型必需，rating类型不需要\n" +
                "- responseTemplate(可选): 响应模板，默认为'选择了${option}'，${option}会被用户选择的label替换",
        required = true,
        type = "object",
        isObject = true
    )
    private InteractionDto interaction;
    
    @Override
    protected String executeInternal() {
        try {
            if (interaction == null) {
                return "Error: 交互配置不能为空";
            }
            
            if (StrUtil.isBlank(interaction.getType())) {
                return "Error: 交互类型(type)不能为空";
            }
            
            if (StrUtil.isBlank(interaction.getPrompt())) {
                return "Error: 提示文本(prompt)不能为空";
            }
            
            if (StrUtil.isBlank(interaction.getResponseTemplate())) {
                interaction.setResponseTemplate(AppConstants.DEFAULT_INTERACTION_RESPONSE_TEMPLATE);
            }
            
            if (!"rating".equals(interaction.getType())) {
                if (interaction.getOptions() == null || interaction.getOptions().isEmpty()) {
                    return "Error: " + interaction.getType() + "类型需要提供options选项列表";
                }
            }
            
            GroupChatMessageSender.StyleText.interaction(engine, currentAgentId, interaction);
            
            return String.format("OK: 已创建%s类型的交互请求 [提示:%s]。等待用户响应...", 
                    interaction.getType(), interaction.getPrompt());
            
        } catch (Exception e) {
            return "Error: 创建交互失败 - " + e.getMessage();
        }
    }
}
