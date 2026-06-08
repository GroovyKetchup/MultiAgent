package cell.ai.agent;

import ai.agent.dto.RespondDto;
import ai.agent.util.ConsolePrintUtil;
import cell.ServiceCellIntf;
import cell.octo.cm.service.IPanelDesignService;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import fe.cmn.panel.PanelContext;
import octo.cm.dto.panelDesign.PanelRoleDto;
import octo.cm.enums.DefaultSystemModule;
import octo.cm.exception.business.DomainException;
import octo.cm.util.UserRoleUtil;
import octocm.domain.dto.DomainDto;
import octocm.domain.observer.OctoDomainOpObserver;
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


}
