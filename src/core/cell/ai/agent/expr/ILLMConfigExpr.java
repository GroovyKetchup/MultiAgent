package cell.ai.agent.expr;

import ai.agent.engine.groupChat.model.definition.LLMConfig;
import ai.agent.service.groupChat.manager.OrchManager;
import cell.CellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cmn.anotation.ClassDeclare;
import cmn.anotation.MethodDeclare;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import octo.cm.util.EasyOperation;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;

@Comment("大模型配置规则")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-11-07", updateTime = "2025-11-07"
)
public interface ILLMConfigExpr extends CellIntf {

    EasyOperation Op = EasyOperation.get();

    @MethodDeclare(
            label = "获取LLM配置列表", how = "", what = "", why = "",
            inputs = {}
    )
    default List<LLMConfig> getLLMConfigList() throws Exception {
        try (IDao dao = IDaoService.newIDao()) {
            List<LLMConfig> llmConfigs = OrchManager.queryAllLLMConfig(dao);

            llmConfigs.sort((o1, o2) -> {
                if (o1.getModel().startsWith("【") && !o2.getModel().startsWith("【")) {
                    return -1;
                }

                return o1.getModel().compareTo(o2.getModel());
            });

            if (Op.isEmpty(llmConfigs)) return new ArrayList<>();
            return llmConfigs;
        }
    }


    @MethodDeclare(
            label = "保存LLM配置", how = "", what = "", why = "",
            inputs = {}
    )
    default void saveLLMConfig(String jsonData) throws Exception {
        LLMConfig llmConfig = null;
        try {
            llmConfig = JSONUtil.toBean(jsonData, LLMConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("数据格式错误，解析失败");
        }

        if (StrUtil.isBlank(llmConfig.getConfigCode())) llmConfig.setConfigCode(IdUtil.fastSimpleUUID());

        try (IDao dao = IDaoService.newIDao()) {
            boolean isSuccess = OrchManager.saveLLMConfig(dao, llmConfig);
            if (!isSuccess) {
                throw new RuntimeException("保存失败");
            }
            dao.commit();
        }


    }

    @MethodDeclare(
            label = "移除LLM配置", how = "", what = "", why = "",
            inputs = {}
    )
    default void deleteLLMConfig(String configCode) throws Exception {
        if (StrUtil.isBlank(configCode)) throw new RuntimeException("要移除的配置编号不得为空");
        try (IDao dao = IDaoService.newIDao()) {
            OrchManager.deleteLLMConfig(dao, configCode);
            dao.commit();
        }

    }


    // ========================= 支撑方法 =========================


}
