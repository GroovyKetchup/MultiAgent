package cell.ai.agent.http;


import ai.agent.constant.HttpConstants;
import ai.agent.dto.RespondDto;
import bap.cells.Cells;
import cell.CellIntf;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cmn.http.anotation.RequestMapping;
import cmn.http.anotation.RequestMethod;
import cmn.http.servlet.mapping.RequestMappingIntf;
import fe.rapidView.anotation.RequestBody;
import org.nutz.dao.entity.annotation.Comment;

@Comment("VotaForge-Evolve-接口")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-07-22", updateTime = "2025-07-22"
)
@RequestMapping(path = HttpConstants.RequestUrlPrefix_Evolve)
public interface IVotaForgeEvolveController extends CellIntf, RequestMappingIntf {

    static IVotaForgeEvolveController get() {
        return Cells.get(IVotaForgeEvolveController.class);
    }

    @MethodDeclare(
            label = "根据智能体ID获取智能体经验",
            what = "",
            why = "",
            how = "",
            inputs = {
                    @InputDeclare(name = "agentId", label = "智能体ID", desc = "")
            }
    )
    @RequestMapping(path = HttpConstants.RequestUrlPath_QueryAgentExperienceByAgentId, method = {RequestMethod.GET})
    RespondDto<Object> queryAgentExperienceByAgentId(String agentId) throws Exception;


    @MethodDeclare(
            label = "保存智能体经验",
            what = "",
            why = "",
            how = "",
            inputs = {
                    @InputDeclare(name = "json", label = "智能体经验JSON", desc = "", nullable = true)
            }
    )
    @RequestBody
    @RequestMapping(path = HttpConstants.RequestUrlPath_SaveAgentExperience, method = {RequestMethod.POST})
    RespondDto<Object> saveAgentExperience(String json) throws Exception;


}
