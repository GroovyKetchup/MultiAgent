package ai.agent.dto.groupChat.workCache;

import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.adur.data.TableData;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Comment("")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2026-01-04", updateTime = "2026-01-04"
)
public class WorkCachePanelEntryOptionDto implements Serializable {
    public static final String PANEL_TABLE = "面板表格";
    public static final String PANEL_FORM = "面板表单";
    public static final String PANEL_PAGE = "面板网页";
    public static final String PANEL_OPTION_NAME_TABLE = "表格名称";
    private static final String PANEL_OPTION_NAME_FORM = "表单名称";
    private static final String PANEL_OPTION_NAME_PAGE = "页面名称";


    private String type;
    private String name;


    // ========================= 支撑方法 =========================


    public static List<WorkCachePanelEntryOptionDto> newDtos(Form panelDesignForm) {
        List<WorkCachePanelEntryOptionDto> dtos = new ArrayList<>();
        if (panelDesignForm == null) return dtos;

        try {
            List<WorkCachePanelEntryOptionDto> tableOptions = convertTdToOptionDto(panelDesignForm.getTable(PANEL_TABLE), PANEL_TABLE, PANEL_OPTION_NAME_TABLE);
            List<WorkCachePanelEntryOptionDto> formOptions = convertTdToOptionDto(panelDesignForm.getTable(PANEL_FORM), PANEL_FORM, PANEL_OPTION_NAME_FORM);
            List<WorkCachePanelEntryOptionDto> pageOptions = convertTdToOptionDto(panelDesignForm.getTable(PANEL_PAGE), PANEL_PAGE, PANEL_OPTION_NAME_PAGE);

            if (CollUtil.isNotEmpty(tableOptions)) dtos.addAll(tableOptions);
            if (CollUtil.isNotEmpty(formOptions)) dtos.addAll(formOptions);
            if (CollUtil.isNotEmpty(pageOptions)) dtos.addAll(pageOptions);

            return dtos;


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static List<WorkCachePanelEntryOptionDto> convertTdToOptionDto(TableData td, String optionType, String optionFieldName) {
        if (td == null || td.isEmtpy() || StrUtil.isBlank(optionType)) return new ArrayList<>();

        List<WorkCachePanelEntryOptionDto> dtos = new ArrayList<>();

        try {
            for (Form row : td.getRows()) {
                String option = row.getString(optionFieldName);
                if (StrUtil.isBlank(option)) continue;

                dtos.add(new WorkCachePanelEntryOptionDto()
                        .setType(optionType)
                        .setName(option));
            }
        } catch (Exception e) {
            return dtos;
        }

        return dtos;
    }

    // ========================= getter/setter =========================

    public String getType() {
        return type;
    }

    public WorkCachePanelEntryOptionDto setType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public WorkCachePanelEntryOptionDto setName(String name) {
        this.name = name;
        return this;
    }
}
