package cell.ai.agent.http;

import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.AgentExperienceDto;
import ai.agent.util.ConsolePrintUtil;
import bap.cells.BasicCell;
import cell.ai.agent.IAgentExperienceService;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cmn.anotation.ClassDeclare;
import cmn.http.servlet.mapping.RequestMappingContext;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import gpf.exception.VerifyException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

@Comment("")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-12", updateTime = "2025-12-12"
)
public class CVotaForgeEvolveController extends BasicCell implements IVotaForgeEvolveController {

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
    public RespondDto<Object> queryAgentExperienceByAgentId(String agentId) throws Exception {
        try {
            if (StrUtil.isBlank(agentId)) throw new VerifyException("智能体ID不得为空");

            try (IDao dao = IDaoService.newIDao()) {

                // 1、查询这个智能体对应的经验
                AgentExperienceDto agentExperienceDto = IAgentExperienceService.get(false).queryByAgentId(dao, agentId);
                if (agentExperienceDto == null)
                    throw new VerifyException(StrUtil.format("未找到ID为[{}]的智能体", agentId));

                // 2、查询全局经验
                AgentExperienceDto globalExperienceDto = IAgentExperienceService.get(false).queryGlobalExperience(dao);
                if (globalExperienceDto != null) {
                    String globalExperienceContent = globalExperienceDto.convertExperienceItemsToText();
                    if (StrUtil.isNotBlank(globalExperienceContent)) {
                        agentExperienceDto.setGlobalExperienceContent(
                                globalExperienceContent
                        );
                    }
                }

                return RespondDto.newSuccess("获取成功",
                        JSONUtil.toJsonStr(agentExperienceDto));

            }

        } catch (Exception e) {
            return RespondDto.newError("获取失败");
        }

    }

    @Override
    public RespondDto<Object> saveAgentExperience(String json) throws Exception {

        try {
            if (StrUtil.isBlank(json))
                throw new VerifyException("智能体经验JSON数据不得为空");

            AgentExperienceDto agentExperienceDto = JSONUtil.toBean(json, AgentExperienceDto.class);
            if (agentExperienceDto == null)
                throw new VerifyException("解析智能体经验JSON失败");

            if (StrUtil.hasBlank(agentExperienceDto.getAgentId(), agentExperienceDto.getAgentName()))
                throw new VerifyException("智能体ID和智能体名称不得为空");

            try (IDao dao = IDaoService.newIDao()) {
                IAgentExperienceService.get(false).save(dao, agentExperienceDto);
                dao.commit();
            }

            return RespondDto.newSuccess("保存成功", null);

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(
                    ExceptionUtils.getStackTrace(e)
            );
            return RespondDto.newError("保存失败: " + e.getMessage());
        }
    }


}

