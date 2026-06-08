package ai.agent.util;

import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import org.nutz.dao.entity.annotation.Comment;

@Comment("模型工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-23", updateTime = "2025-09-23"
)
public class FormModelUtil {

    public static final String TEMPLATE_MODEL_ID_PROCESS = "octocm.md.{}.process.{}_CM";
    public static final String TEMPLATE_MODEL_ID_NORMAL = "octocm.md.{}.{}_CM";


    // 构建面板设计模型ID
    public static String buildPanelFormModelId(String busDomainCode, String panelCode) {
        if (StrUtil.hasBlank(busDomainCode, panelCode)) throw new RuntimeException("业务域与面板编号不得为空");
        panelCode = panelCode.replaceAll("IML", "iML");
        return StrUtil.format(TEMPLATE_MODEL_ID_NORMAL, busDomainCode, panelCode);

    }

    // 构建面板设计模型ID
    public static String buildPanelFormModelIdByCmName(String busDomainCode, String panelCode) {
        if (StrUtil.hasBlank(busDomainCode, panelCode)) throw new RuntimeException("业务域与面板编号不得为空");
        panelCode = panelCode.replaceAll("_CM", "");
        panelCode = panelCode.replaceAll("IML", "iML");
        return StrUtil.format(TEMPLATE_MODEL_ID_NORMAL, busDomainCode, panelCode);

    }

    // 构建面板设计模型ID
    public static String buildPanelProcessFormModelId(String panelCode) {
        String[] split = null;
        if (StrUtil.isBlank(panelCode) || (split = panelCode.split("_")).length != 3) {
            throw new RuntimeException(StrUtil.format("面板编号[{}]为空或缺失了业务域编号",
                    panelCode));
        }
        String busDomainCode = split[0];
        String nonDomainPanelCode = StrUtil.format("{}_{}", split[1], split[2]);

        return StrUtil.format(TEMPLATE_MODEL_ID_PROCESS, busDomainCode, nonDomainPanelCode);

    }

    public static String deleteProcessFormModelToNormal(String formModelId) {
        // I: octocm.md.WEH.process.IML_00003_CM
        // O: octocm.md.WEH.IML_00003_CM
        if (StrUtil.isBlank(formModelId)) return formModelId;
        return formModelId.replaceAll("process\\.I", "i");

    }

}
