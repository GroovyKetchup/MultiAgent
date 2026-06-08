package ai.agent.service.groupChat.manager;


import ai.agent.engine.groupChat.model.definition.LLMConfig;
import cell.cdao.IDao;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import octo.cm.util.EasyOperation;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpressionGroup;

import java.util.LinkedList;
import java.util.List;

@Comment("Orch管理类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-11-07", updateTime = "2025-11-07"
)
public class OrchManager {

    private static final EasyOperation Op = EasyOperation.get();

    // Orch的大模型配置
    private static final String FORM_MODEL_ID_LLM_CONFIG = "gpf.md.orch.LLM_pei_zhi";
    private static final String FORM_MODEL_ID_LLM_NAMES = "gpf.md.orch.LLM_lie_biao";

    // 获取系统内的所有LLM配置
    public static List<LLMConfig> queryAllLLMConfig(IDao dao) {
        List<LLMConfig> resultList = new LinkedList<>();
        try {

            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, FORM_MODEL_ID_LLM_CONFIG, null, 1, Integer.MAX_VALUE,
                    false, false);

            for (Form form : queryRs.getDataList()) {
                String configCode = form.getString(Form.Code);
                String configName = form.getString("名称");
                String baseUrl = form.getString("baseUrl");
                String apiKey = form.getString("apiKey");
                if (StrUtil.hasBlank(baseUrl, apiKey)) continue;
                AssociationData model = form.getAssociation("model");
                if (model == null) continue;

                String modelName = model.getForm().getString("名称");
                if (StrUtil.isBlank(modelName)) continue;

                if (modelName.contains("OhMyGPT-")) {
                    modelName = modelName.replaceAll("OhMyGPT-", "");
                }
                Long maxTokens = form.getLong("maxTokens");
                Double temperature = form.getDouble("temperature");
                Double topP = form.getDouble("topP");
                Double presencePenalty = form.getDouble("presencePenalty");
                Double frequencyPenalty = form.getDouble("frequencyPenalty");


                resultList.add(new LLMConfig(
                        configCode,
                        configName,
                        baseUrl,
                        apiKey,
                        modelName,
                        maxTokens,
                        temperature,
                        topP,
                        presencePenalty,
                        frequencyPenalty
                ));

            }

            return resultList;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static List<LLMConfig> removeApiKey(List<LLMConfig> llmConfigs) {
        if (Op.isEmpty(llmConfigs)) return llmConfigs;

        for (LLMConfig llmConfig : llmConfigs) {
            llmConfig.setApiKey("");
        }

        return llmConfigs;

    }


    // 保存LLM配置
    public static boolean saveLLMConfig(IDao dao, LLMConfig llmConfig) throws Exception {
        if (llmConfig == null) return false;
        String configCode = llmConfig.getConfigCode();
        if (StrUtil.isBlank(configCode)) return false;
        Form form = Op.queryFormByCondition(dao, FORM_MODEL_ID_LLM_CONFIG, Form.Code, configCode, null);
        boolean isCreate = false;
        if (form == null) {
            isCreate = true;
            form = Op.newForm(FORM_MODEL_ID_LLM_CONFIG);
        }
        form.setAttrValue(Form.Code, llmConfig.getConfigCode());
        form.setAttrValue("名称", llmConfig.getConfigName());
        form.setAttrValue("baseUrl", llmConfig.getBaseUrl());
        form.setAttrValue("apiKey", llmConfig.getApiKey());
        form.setAttrValue("maxTokens", llmConfig.getMaxTokens());
        form.setAttrValue("temperature", llmConfig.getTemperature());
        form.setAttrValue("topP", llmConfig.getTopP());
        form.setAttrValue("presencePenalty", llmConfig.getPresencePenalty());
        form.setAttrValue("frequencyPenalty", llmConfig.getFrequencyPenalty());

        AssociationData modelAc = doGetOrCreateModelAcObj(dao, llmConfig.getModel());
        if (modelAc != null) {
            form.setAttrValue("model", modelAc);
        }

        if (isCreate) {
            IFormMgr.get().createForm(dao, form);
        } else {
            IFormMgr.get().updateForm(dao, form);
        }

        return true;
    }

    // 删除LLM配置
    public static void deleteLLMConfig(IDao dao, String configCode) throws Exception {
        Cnd cnd = Cnd.NEW();
        cnd.where().andEquals(Form.Code, configCode);
        IFormMgr.get().deleteForm(dao, FORM_MODEL_ID_LLM_CONFIG, cnd);
    }

    // ========================= 支撑方法 =========================

    private static AssociationData doGetOrCreateModelAcObj(IDao dao, String model) {
        try {
            Cnd cnd = Cnd.NEW();
            cnd.where().and(
                    new SqlExpressionGroup()
                            .orEquals(Form.Code, model)
                            .orEquals(Op.getFieldCode("名称"), model));

            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, FORM_MODEL_ID_LLM_NAMES, cnd,
                    1, 1, false, false);
            if (!queryRs.isEmpty()) return Op.toAssociationData(queryRs.getDataList().get(0));

            Form form = Op.newForm(FORM_MODEL_ID_LLM_NAMES);
            form.setAttrValue(Form.Code, model).setAttrValue("名称", model);

            IFormMgr.get().createForm(dao, form);

            return Op.toAssociationData(form);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


}
