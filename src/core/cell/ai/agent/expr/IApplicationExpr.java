package cell.ai.agent.expr;

import cell.CellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octocm.workbench.app.IApplicationDeploy;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cmn.dto.Progress;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import gpf.adur.data.DataType;
import gpf.adur.data.Form;
import gpf.adur.data.FormField;
import gpf.adur.data.FormModel;
import gpf.adur.user.User;
import octo.cm.dto.app.IpWhitelistConfigDto;
import octo.cm.util.ApplicationUtil;
import octo.cm.util.EasyOperation;
import octo.cm.util.FormToJsonConversionUtil;
import octo.cm.util.JsonToFormConversionUtil;
import octocm.domain.observer.OctoDomainOpObserver;
import octocm.workbench.dto.app.ApplicationDeployDto;
import org.nutz.dao.entity.annotation.Comment;

import java.util.HashMap;
import java.util.Map;

@Comment("应用规则")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-11-07", updateTime = "2025-11-07"
)
public interface IApplicationExpr extends CellIntf {

    EasyOperation Op = EasyOperation.get();


    @MethodDeclare(
            label = "获取业务域默认应用", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "operator", label = "", desc = "", exampleValue = "$operator$", nullable = true),
                    @InputDeclare(name = "operatorCode", label = "", desc = "", exampleValue = "$operatorCode$", nullable = true),
                    @InputDeclare(name = "env", label = "", desc = "", exampleValue = "$env$", nullable = true),
                    @InputDeclare(name = "busDomainCode", label = "业务域编号", desc = "")
            }
    )
    default JSONObject getBusDomainDefaultApplicationConfig(User operator,String operatorCode,Map<String, Object> env,String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw new RuntimeException("业务域编号不得为空");

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
        if (observer == null) throw new RuntimeException(StrUtil.format("不存在业务域编号[{}]", busDomainCode));

        try (IDao dao = IDaoService.newIDao()) {

            // 通过业务域去搜索
            String applicationCode = ApplicationUtil
                    .getDefaultPublishApplicationCode(observer);

            // 如果搜索结果为空，那么就设置一下，默认的应用就是业务域编号（这个是默认会添加的）
            if (applicationCode == null) {
                ApplicationUtil.setDefaultPublishApplicationCode(observer, observer.getDomainCode());
                applicationCode = observer.getDomainCode();
            }

            Form appForm = ApplicationUtil.queryApplicationFormByAppCode(dao, applicationCode);

            if (appForm == null) {
                throw new RuntimeException(StrUtil.format("不存在编号为[{}]的应用", applicationCode));
            }

            appForm.setAttrValue(FormToJsonConversionUtil.PREFIX_NEED_UUID, true);
            JSONObject appConfig = FormToJsonConversionUtil
                    .convert(appForm);
            ApplicationUtil.exposeIpWhitelistConfig(appConfig, appForm);
            return appConfig;
        }
    }


    @MethodDeclare(
            label = "保存应用配置", how = "", what = "", why = "",
            inputs = {}
    )
    default void saveApplicationConfig(String busDomainCode, String appCode, String jsonData) throws Exception {

        if (StrUtil.isBlank(appCode)) throw new RuntimeException("应用编号不得为空");
        if (StrUtil.isBlank(jsonData)) throw new RuntimeException("应用配置数据不得为空");

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
        if (observer == null) throw new RuntimeException(StrUtil.format("不存在业务域编号[{}]", busDomainCode));


        JSONObject jsonObject = null;
        try {
            jsonObject =
                    JSONUtil.parseObj(jsonData);
        } catch (Exception e) {
            throw new RuntimeException("应用配置数据的JSON数据有误");
        }


        try (IDao dao = IDaoService.newIDao()) {
            IpWhitelistConfigDto ipWhitelistConfig = ApplicationUtil.takeIpWhitelistConfig(jsonObject);

            // 通过业务域去搜索
            String applicationCode = ApplicationUtil
                    .getDefaultPublishApplicationCode(observer);

            Form appForm = ApplicationUtil.queryApplicationFormByAppCode(dao, applicationCode);

            if (appForm == null) throw new RuntimeException(StrUtil.format("不存在编号为[{}]的应用", applicationCode));

            Form form = JsonToFormConversionUtil.convert(appForm, jsonObject);

            if (form == null) throw new RuntimeException("应用配置数据有误，无法进行转换");

            if (ipWhitelistConfig != null) {
                ApplicationUtil.setIpWhitelistConfig(form, ipWhitelistConfig);
            }

            IFormMgr.get().updateForm(null, dao, form, observer);
            IApplicationDeploy.get().deploy(Progress.newOutput(), dao, form, observer);

            ApplicationUtil
                    .setDefaultPublishApplicationCode(observer, form.getString(ApplicationDeployDto.sName));

            dao.commit();

        }

    }


    // ========================= 支撑方法 =========================


    // 移除所有附件字段
    default Form removeAllAttachmentField(IDao dao, Form form) {
        if (form == null) return null;

        try {
            Map<String, Object> oldData = form.getData();
            Map<String, Object> newData = new HashMap<>();

            if (oldData == null || oldData.isEmpty()) return form;

            FormModel formModel = IFormMgr.get().queryFormModel(form.getFormModelId());
            if (formModel == null) return form;

            Map<String, FormField> fieldNameMap = formModel.getFieldNameMap();

            for (Map.Entry<String, Object> entry : oldData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                FormField formField = fieldNameMap.get(key);
                if (formField == null) continue;

                DataType dataTypeEnum = formField.getDataTypeEnum();
                // 屏蔽所有附件字段
                if (dataTypeEnum.equals(DataType.Attach) || dataTypeEnum.equals(DataType.WebAttach)) {
                    continue;
                }

                newData.put(key, value);
            }

            form.setData(newData);

            return form;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
