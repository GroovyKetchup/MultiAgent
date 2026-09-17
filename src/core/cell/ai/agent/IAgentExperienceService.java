package cell.ai.agent;


import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.AgentExperienceDto;
import ai.agent.util.ConsolePrintUtil;
import bap.cells.Cells;
import cell.cdao.IDao;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.exception.VerifyException;
import octo.cm.util.EasyOperation;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static ai.agent.constant.HttpConstants.*;

@Comment("智能体经验库服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-10", updateTime = "2025-09-10"
)
// cell.ai.agent.IAgentExperienceService
public interface IAgentExperienceService extends IGroupChatBasicService {
    static IAgentExperienceService get(boolean enableRemoterMode) {
        IAgentExperienceService service = Cells.get(IAgentExperienceService.class, enableRemoterMode);
        service.initCell(enableRemoterMode);
        return service;
    }


    EasyOperation Op = EasyOperation.get();
    String GLOBAL_EXPERIENCE_AGENT_ID_FLAG = "*";
    String REMOTE_BASEURL = "https://demo.kwaidoo.com/VF_DEV/" + RequestUrlPrefix_Evolve;
    String REMOTE_URL_QUERY_BY_AGENT_ID = REMOTE_BASEURL + RequestUrlPath_QueryAgentExperienceByAgentId;
    String REMOTE_URL_SAVE = REMOTE_BASEURL + RequestUrlPath_SaveAgentExperience;

    AtomicBoolean enableRemoterMode = new AtomicBoolean(false);

    @Override
    default void initCell(Object... params) {
        IGroupChatBasicService.super.initCell(params);

        if (params.length > 0) {
            Boolean param = (Boolean) params[0];
            enableRemoterMode.set(BooleanUtil.isTrue(param));
        }

    }


    // 获取智能体经验列表
    // TODO 为智能体列表做缓存
    default List<AgentExperienceDto> queryAll(IDao dao, boolean enableCache) throws Exception {

        ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, AgentExperienceDto.FORM_MODEL_ID, null,
                1, Integer.MAX_VALUE, false, false);

        if (queryRs.isEmpty()) return new ArrayList<>();
        List<AgentExperienceDto> experienceDtos = Lists.newArrayList();
        for (Form form : queryRs.getDataList()) {
            AgentExperienceDto dto = AgentExperienceDto.newDto(form);
            if (dto == null) continue;
            experienceDtos.add(dto);
        }


        return experienceDtos;

    }

    // 根据智能体ID获取智能体经验
    default AgentExperienceDto queryByAgentId(IDao dao, String agentId) throws Exception {

        if (enableRemoterMode.get()) {
            ConsolePrintUtil.printGreenLn(
                    StrUtil.format("[智能体经验][REMOTE] 查询智能体ID: {}", agentId)
            );
            String result = HttpUtil.get(REMOTE_URL_QUERY_BY_AGENT_ID, MapUtil.of("agentId", agentId));
            if (StrUtil.isBlank(result)) return null;

            try {
                RespondDto bean = JSONUtil.toBean(result, RespondDto.class);
                if (bean != null && bean.getSuccess()) {
                    return JSONUtil.toBean(JSONUtil.toJsonStr(bean.getData()),
                            AgentExperienceDto.class);
                }
            } catch (Exception e) {
                return null;
            }


        }

        Form form = queryFormByAgentId(dao, agentId);
        if (form == null) return null;
        return AgentExperienceDto.newDto(form);
    }

    // 查询全局智能体经验
    default AgentExperienceDto queryGlobalExperience(IDao dao) throws Exception {
        return queryByAgentId(dao, GLOBAL_EXPERIENCE_AGENT_ID_FLAG);
    }


    // 保存智能体经验
    default void save(IDao dao, AgentExperienceDto dto) throws Exception {
        if (dto == null) return;
        // FIXME 晓斌的接口不支持POST-JSON
        if (enableRemoterMode.get()) {
            HttpRequest post = HttpUtil.createPost(REMOTE_URL_SAVE);
            post.contentType("multipart/form-data; boundary=<calculated when response is sent>");

            Map<String, Object> form = new HashMap<>();
            form.put("json", JSONUtil.toJsonStr(dto));
            post.form(form);
            String result = post.execute().body();

            ConsolePrintUtil.printGreenLn(
                    StrUtil.format("[智能体经验][REMOTE] 保存结果: {}", result)
            );
            return;
        }


        String agentId = dto.getAgentId();
        if (StrUtil.isBlank(agentId)) throw new VerifyException("智能体ID不得为空");
        Form old = queryFormByAgentId(dao, agentId);

        Form form = dto.toForm();
        if (old == null) {
            IFormMgr.get().createForm(dao, form);
        } else {
            form.setUuid(old.getUuid())
                    .setAttrValue(Form.Code, form.getAttrValue(Form.Code));
            IFormMgr.get().updateForm(dao, form);
        }

    }

    // ========================= 支撑方法 =========================

    default Form queryFormByAgentId(IDao dao, String agentId) throws Exception {
        return Op.queryFormByCondition(dao, AgentExperienceDto.FORM_MODEL_ID, AgentExperienceDto.FIELD_NAME_AGENT_ID, agentId, null);
    }


}
