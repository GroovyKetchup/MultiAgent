package ai.agent.dto.groupChat.workCache;

import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import octo.cm.util.PanelCategoryUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("面板设计Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-10", updateTime = "2025-09-10"
)
public class WorkCachePanelDesignDto implements Serializable {

    // 面板编号
    private String panelCode;
    // 面板名称
    private String panelName;
    // 面板分类
    private String panelCategory;
    // 面板描述
    private String panelDesc;
    // 面板数据结构
    private String panelDataStructure;
    // 面板入口选项
    private List<WorkCachePanelEntryOptionDto> panelEntryOptions;

    // 交付状态
    private WorkCachePanelDeliverStatusDto panelDeliverStatus;

    // ========================= 支撑方法 =========================

    // 从Form中转换
    public static WorkCachePanelDesignDto newDto(Form panelDesignForm) {
        if (panelDesignForm == null) return null;

        try {
            String panelCode = panelDesignForm.getString("面板编号");
            String panelName = panelDesignForm.getString("面板名称");
            String panelCategory = PanelCategoryUtil.getPanelCategory(panelDesignForm);
            String panelDescription = panelDesignForm.getString("面板描述");

            List<WorkCachePanelEntryOptionDto> panelEntryOptions = WorkCachePanelEntryOptionDto
                    .newDtos(panelDesignForm);
            if (StrUtil.hasBlank(panelCode, panelName)) return null;

            return new WorkCachePanelDesignDto()
                    .setPanelCode(panelCode)
                    .setPanelName(panelName)
                    .setPanelCategory(panelCategory)
                    .setPanelDesc(panelDescription)
                    .setPanelDataStructure("")
                    .setPanelEntryOptions(panelEntryOptions)
                    .setPanelDeliverStatus(
                            new WorkCachePanelDeliverStatusDto()
                                    .setDeliverType(WorkCachePanelDeliverStatusDto.DELIVER_TYPE_PROTOTYPE)
                                    .setDeliverStatus(WorkCachePanelDeliverStatusDto.DELIVER_STATUS_PENDING)
                                    .setTestStatus(WorkCachePanelDeliverStatusDto.TEST_STATUS_PENDING)
                    )
                    ;

        } catch (Exception e) {
            return null;
        }

    }


    // ========================= getter/setter =========================

    public String getPanelCode() {
        return panelCode;
    }

    public WorkCachePanelDesignDto setPanelCode(String panelCode) {
        this.panelCode = panelCode;
        return this;
    }

    public String getPanelName() {
        return panelName;
    }

    public WorkCachePanelDesignDto setPanelName(String panelName) {
        this.panelName = panelName;
        return this;
    }

    public String getPanelDesc() {
        return panelDesc;
    }

    public WorkCachePanelDesignDto setPanelDesc(String panelDesc) {
        this.panelDesc = panelDesc;
        return this;
    }

    public String panelCode() {
        return panelCode;
    }

    public String panelName() {
        return panelName;
    }

    public String panelCategory() {
        return panelCategory;
    }

    public WorkCachePanelDesignDto setPanelCategory(String panelCategory) {
        this.panelCategory = panelCategory;
        return this;
    }

    public String panelDesc() {
        return panelDesc;
    }

    public String panelDataStructure() {
        return panelDataStructure;
    }

    public WorkCachePanelDesignDto setPanelDataStructure(String panelDataStructure) {
        this.panelDataStructure = panelDataStructure;
        return this;
    }

    public List<WorkCachePanelEntryOptionDto> getPanelEntryOptions() {
        return panelEntryOptions;
    }

    public WorkCachePanelDesignDto setPanelEntryOptions(List<WorkCachePanelEntryOptionDto> panelEntryOptions) {
        this.panelEntryOptions = panelEntryOptions;
        return this;
    }

    public WorkCachePanelDeliverStatusDto getPanelDeliverStatus() {
        return panelDeliverStatus;
    }

    public WorkCachePanelDesignDto setPanelDeliverStatus(WorkCachePanelDeliverStatusDto panelDeliverStatus) {
        this.panelDeliverStatus = panelDeliverStatus;
        return this;
    }
}
