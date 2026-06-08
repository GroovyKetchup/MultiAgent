package ai.agent.engine.groupChat.tool.impl.VotaForge;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.dto.RespondDto;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import cell.ai.agent.IGroupChatRoomService;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.dc.basic.form.define.ApplicationDefine;
import octo.cm.util.ApplicationUtil;
import octo.cm.util.EasyOperation;
import octocm.domain.dto.DomainDto;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.Cnd;

@ToolDeclare(
    name = "SetApplicationNameTool",
    cnName = "设置应用名称",
    description = "系统最终生成出的应用是随机字符，那么就需要使用这个工具去设置应用的名称。通过本工具, 你可以修改VotaForge所构建应用对应的应用名称",
    scope = ToolScope.GROUP_CHAT
)
public class SetApplicationNameTool extends AbsGroupChatTool {

    public static final EasyOperation Op = EasyOperation.get();

    @ParamDeclare(description = "应用名称，限制在7个中文字符或14个英文字符以内", required = true)
    private String appName;

    @Override
    protected String executeInternal() {
        if (StrUtil.isBlank(appName)) {
            return RespondDto.newStrError("请输入正确的应用名称!");
        }

        try (IDao dao = IDaoService.newIDao()) {
            DomainDto busDomain = engine.getBusDomain();

            ApplicationUtil.setAppName(dao, new OctoDomainOpObserver(busDomain), appName);
            IGroupChatRoomService.get().changeRoomName(dao, busDomain.getDomainCode(), appName);
            dao.commit();
        } catch (Exception e) {
            Op.logException(e);
            throw new RuntimeException("修改应用名称失败, 错误原因:" + e.getMessage(), e);
        }

        return RespondDto.newStrSuccess("应用名称修改成功", null);
    }


    private void doSetAppName(IDao dao, DomainDto busDomain, String groupInstanceId, String appName) throws Exception {

        String formModelId = ApplicationDefine.FormModelId;

        String applicationCode = ApplicationUtil.getDefaultPublishApplicationCode(new OctoDomainOpObserver(busDomain));
        if (applicationCode == null) {
            applicationCode = busDomain.getDomainCode();
        }

        Cnd queryCnd = Cnd.NEW();
        queryCnd.where().andEquals(Form.Code, applicationCode);

        ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, formModelId, queryCnd, 1, 1, true, true);
        if (queryRs.isEmpty()) throw new RuntimeException("无法找到当前实例对应的应用");

        Form appForm = queryRs.getDataList().get(0);
        appForm.setAttrValue(ApplicationDefine.sLabel, appName);

        IFormMgr.get().updateForm(dao, appForm);


    }
}

