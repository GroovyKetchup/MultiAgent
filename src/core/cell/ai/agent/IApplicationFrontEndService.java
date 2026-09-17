package cell.ai.agent;

import ai.agent.dto.RespondDto;
import ai.agent.util.ConsolePrintUtil;
import cell.ServiceCellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octocm.workbench.app.IApplicationDeploy;
import cell.octo.cm.service.IPanelDesignService;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import fe.cmn.panel.PanelContext;
import gpf.adur.data.Form;
import octo.cm.dto.panelDesign.PanelRoleDto;
import octo.cm.enums.DefaultSystemModule;
import octo.cm.exception.business.DomainException;
import octo.cm.util.ApplicationUtil;
import octo.cm.util.EasyOperation;
import octo.cm.util.FormToJsonConversionUtil;
import octo.cm.util.JsonToFormConversionUtil;
import octo.cm.util.UserRoleUtil;
import octocm.domain.dto.DomainDto;
import octocm.domain.observer.OctoDomainOpObserver;
import octocm.workbench.dto.app.ApplicationDeployDto;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.util.LinkedHashMap;
import java.util.List;

@Comment("应用前端服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-31", updateTime = "2025-12-31"
)
// cell.ai.agent.IApplicationFrontEndService
public interface IApplicationFrontEndService extends ServiceCellIntf {

    EasyOperation Op = EasyOperation.get();


    // 获取系统模块列表
    @SuppressWarnings("unused")
    default RespondDto getSystemModules(PanelContext panelContext) {
        return RespondDto.newSuccess("获取系统模块成功", JSONUtil.toJsonStr(DefaultSystemModule.values()));
    }

    // 添加系统模块
    @SuppressWarnings("unused")
    default RespondDto addSystemModules(PanelContext panelContext, String groupChatInstId, String moduleName, LinkedHashMap moduleParamObj) {

        try {

            if (StrUtil.isBlank(groupChatInstId)) throw DomainException.Builder.busDomainCodeEmpty();
            DomainDto busDomain = IVotaForgeService.get().getBusDomain(groupChatInstId);
            if (busDomain == null) throw DomainException.Builder.notFoundWithCode(groupChatInstId);

            DefaultSystemModule systemModule = DefaultSystemModule.valueOf(moduleName);
            if (systemModule == null) throw new RuntimeException(StrUtil.format("不支持组件[{}]", moduleName));

            ConsolePrintUtil.printGreenLn("moduleParamObj:" + JSONUtil.toJsonStr(moduleParamObj));

            systemModule.setParams(moduleParamObj);

            OctoDomainOpObserver opObserver = new OctoDomainOpObserver(busDomain);
            IPanelDesignService.get().loadDefaultPanelDesign(opObserver,
                    systemModule);

            return RespondDto.newSuccess("添加系统模块成功", null);

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
            return RespondDto.newError("添加系统模块失败");
        }

    }

    // 获取角色列表
    @SuppressWarnings("unused")
    default RespondDto queryRoles(PanelContext panelContext, String groupChatInstId) {
        try {

            if (StrUtil.isBlank(groupChatInstId)) throw DomainException.Builder.busDomainCodeEmpty();
            DomainDto busDomain = IVotaForgeService.get().getBusDomain(groupChatInstId);
            if (busDomain == null) throw DomainException.Builder.notFoundWithCode(groupChatInstId);

            List<PanelRoleDto> roles = UserRoleUtil.queryRoles(new OctoDomainOpObserver(busDomain));

            return RespondDto.newSuccess("获取角色列表成功", JSONUtil.toJsonStr(roles));
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
            return RespondDto.newError("获取角色失败:" + e.getMessage());
        }

    }


    // ========================= 应用管理（多应用） =========================

    @SuppressWarnings("unused")
    default RespondDto getApplicationList(PanelContext panelContext, String groupChatInstId) {
        try {
            OctoDomainOpObserver observer = requireBusDomainObserver(groupChatInstId);
            String defaultAppCode = resolveDefaultAppCode(observer);

            try (IDao dao = IDaoService.newIDao()) {
                List<Form> forms = ApplicationUtil.queryApplicationForms(dao, observer, false);
                if (Op.isEmpty(forms)) {
                    return RespondDto.newSuccess("获取应用列表成功", JSONUtil.toJsonStr(CollUtil.newArrayList()));
                }

                JSONArray resultList = new JSONArray();
                for (Form form : forms) {
                    String code = resolveAppCode(form);
                    JSONObject item = new JSONObject();
                    item.set("code", code);
                    item.set("label", form.getString("标签"));
                    item.set("systemName", form.getString(ApplicationDeployDto.sSystemName));
                    item.set("isDefault", StrUtil.equals(defaultAppCode, code));
                    resultList.add(item);
                }
                return RespondDto.newSuccess("获取应用列表成功", JSONUtil.toJsonStr(resultList));
            }
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
            return RespondDto.newError("获取应用列表失败:" + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    default RespondDto getApplicationConfig(PanelContext panelContext, String groupChatInstId, String appCode) {
        try {
            OctoDomainOpObserver observer = requireBusDomainObserver(groupChatInstId);
            String targetAppCode = StrUtil.isBlank(appCode) ? resolveDefaultAppCode(observer) : appCode;

            try (IDao dao = IDaoService.newIDao()) {
                Form appForm = ApplicationUtil.queryApplicationFormByAppCode(dao, targetAppCode);
                if (appForm == null) {
                    throw new RuntimeException(StrUtil.format("不存在编号为[{}]的应用", targetAppCode));
                }

                appForm.setAttrValue(FormToJsonConversionUtil.PREFIX_NEED_UUID, true);
                JSONObject config = FormToJsonConversionUtil.convert(appForm);
                return RespondDto.newSuccess("获取应用配置成功", JSONUtil.toJsonStr(config));
            }
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
            return RespondDto.newError("获取应用配置失败:" + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    default RespondDto saveApplicationConfig(PanelContext panelContext, String groupChatInstId,
                                             String appCode, String jsonData) {
        try {
            if (StrUtil.isBlank(appCode)) throw new RuntimeException("应用编号不得为空");
            if (StrUtil.isBlank(jsonData)) throw new RuntimeException("应用配置数据不得为空");

            OctoDomainOpObserver observer = requireBusDomainObserver(groupChatInstId);

            JSONObject jsonObject;
            try {
                jsonObject = JSONUtil.parseObj(jsonData);
            } catch (Exception e) {
                throw new RuntimeException("应用配置数据的JSON数据有误");
            }

            try (IDao dao = IDaoService.newIDao()) {
                Form appForm = ApplicationUtil.queryApplicationFormByAppCode(dao, appCode);
                if (appForm == null) {
                    throw new RuntimeException(StrUtil.format("不存在编号为[{}]的应用", appCode));
                }

                Form form = JsonToFormConversionUtil.convert(dao,appForm, jsonObject);
                if (form == null) throw new RuntimeException("应用配置数据有误，无法进行转换");

                IFormMgr.get().updateForm(null, dao, form, observer);
                IApplicationDeploy.get().deploy(Progress.newOutput(), dao, form, observer);
                dao.commit();
            }

            return RespondDto.newSuccess("保存应用配置成功", null);
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
            return RespondDto.newError("保存应用配置失败:" + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    default RespondDto setDefaultApplication(PanelContext panelContext, String groupChatInstId, String appCode) {
        try {
            if (StrUtil.isBlank(appCode)) throw new RuntimeException("应用编号不得为空");

            OctoDomainOpObserver observer = requireBusDomainObserver(groupChatInstId);

            try (IDao dao = IDaoService.newIDao()) {
                Form appForm = ApplicationUtil.queryApplicationFormByAppCode(dao, appCode);
                if (appForm == null) {
                    throw new RuntimeException(StrUtil.format("不存在编号为[{}]的应用", appCode));
                }
                ApplicationUtil.setDefaultPublishApplicationCode(observer, appCode);
            }

            return RespondDto.newSuccess("设置默认应用成功", null);
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
            return RespondDto.newError("设置默认应用失败:" + e.getMessage());
        }
    }


    // ========================= 支撑方法 =========================

    default OctoDomainOpObserver requireBusDomainObserver(String groupChatInstId) {
        if (StrUtil.isBlank(groupChatInstId)) throw DomainException.Builder.busDomainCodeEmpty();
        DomainDto busDomain = IVotaForgeService.get().getBusDomain(groupChatInstId);
        if (busDomain == null) throw DomainException.Builder.notFoundWithCode(groupChatInstId);
        return new OctoDomainOpObserver(busDomain);
    }

    default String resolveDefaultAppCode(OctoDomainOpObserver observer) throws Exception {
        String applicationCode = ApplicationUtil.getDefaultPublishApplicationCode(observer);
        if (applicationCode == null) {
            ApplicationUtil.setDefaultPublishApplicationCode(observer, observer.getDomainCode());
            applicationCode = observer.getDomainCode();
        }
        return applicationCode;
    }

    default String resolveAppCode(Form form) throws Exception {
        String name = form.getString(ApplicationDeployDto.sName);
        if (StrUtil.isNotBlank(name)) return name;
        return form.getString(Form.Code);
    }


}
