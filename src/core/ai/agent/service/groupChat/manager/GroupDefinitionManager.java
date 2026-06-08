package ai.agent.service.groupChat.manager;

import ai.agent.constant.AppConstants;
import ai.agent.constant.GroupChatConstants;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.util.ConsolePrintUtil;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Group Definition Manager - Manages group definitions by code
 */
public class GroupDefinitionManager {
    
    // Static registry of group definitions
    private static final Map<String, GroupDefinition> definitions = new LinkedHashMap<>();
    
    static {
        GroupDefinition defaultGroupDefinition = AppConstants.DEFAULT_GROUP_DEFINITION;
        definitions.put(GroupChatConstants.DefaultGroupId,defaultGroupDefinition);
    }
    
    /**
     * Get group definition by code
     */
    public static GroupDefinition getDefinition(String code) {
        return  definitions.get(code);
    }

    // 尝试更新定义
    public static void tryUpdateDefinition(GroupDefinition definition) {
        if(definition == null) return;

        // 1、拿到智能体定义
        List<AgentDefinition> agentDefinitions = definition.getAgentDefinitions();
        if (CollUtil.isEmpty(agentDefinitions)) return;

        Map<String, AgentDefinition> agentDefinitionMap = agentDefinitions.stream()
                .collect(Collectors.toMap(AgentDefinition::getAgentId,
                        agentDefinition -> agentDefinition));

        // 2、从智能体管理中读取用户定义的内容，进行填充到智能体定义中
        try (IDao dao = IDaoService.newIDao()) {

            ResultSet<Form> queryRs = IFormMgr.get()
                    .queryFormPage(dao, AgentDefinition.FORM_MODEL_ID, null, 1, Integer.MAX_VALUE, true, true);
            if (!queryRs.isEmpty()) {
                for (Form agentDefinitionForm : queryRs.getDataList()) {

                    String agentId = agentDefinitionForm.getString("智能体ID");
                    if (StrUtil.isBlank(agentId) || !agentDefinitionMap.containsKey(agentId)) continue;

                    AgentDefinition agentDefinition = agentDefinitionMap.get(agentId);
                    if (agentDefinition == null) continue;

                    agentDefinition.updateByForm(agentDefinitionForm);


                }


            }

        }catch (Exception e){

            ConsolePrintUtil.printRedLn(
                    ExceptionUtils.getFullStackTrace(e)
            );



        }

    }

    
    /**
     * Register a new group definition
     */
    public static void registerDefinition(GroupDefinition definition) {
        if (definition != null && definition.getGroupCode() != null) {
            definitions.put(definition.getGroupCode(), definition);
        }
    }
    
    /**
     * Get first available definition (for testing)
     */
    public static GroupDefinition getFirstDefinition() {
        return definitions.values().stream().findFirst().orElse(null);
    }
    
}
