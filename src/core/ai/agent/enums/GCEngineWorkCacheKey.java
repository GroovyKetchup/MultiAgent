package ai.agent.enums;

public enum GCEngineWorkCacheKey {

    CANVAS_CURRENT_DATA("当前画板中的数据"),
    LAST_USER_UPLOAD_DOCUMENT("上次用户上传的文档"),
    LAST_IMPORTED_DOCUMENT_CODES("上次用户导入的文档编号列表"),
    LAST_DOCUMENT_PARSE_TASK_PROCESS_IDS("上次文档解析任务的任务ID列表"),
    LAST_DOCUMENT_PARSE_TASK_RESULTS("上次文档解析任务的任务解析结果列表"),
    LAST_ROUGH_DELIVERY_GENERATED_PANEL_DESIGNS("上次原型交付生成的面板设计列表"),
    CURRENT_CANVAS_STATUS("当前画布信息"),
    TODO_LIST("待办事项列表");


    private String cnName;

    GCEngineWorkCacheKey(String cnName) {
        this.cnName = cnName;
    }

    public String getCnName() {
        return cnName;
    }

    @Override
    public String toString() {
        return name();
    }

}
