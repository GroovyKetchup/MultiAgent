package ai.agent.dto.groupChat.canvas;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.Map;

@Comment("当前画布状态")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.1",
        createTime = "2025-11-23", updateTime = "2025-12-17"
)
public class CanvasStatus implements Serializable {
    // 画布类型
    private String canvasType;
    // 画布参数
    private Map<String, Object> canvasParams;
    // 画布打开者
    private String canvasOpener;
    // 所属子会话ID，null表示主会话画布
    private String subSessionId;

    public String getCanvasType() {
        return canvasType;
    }

    public CanvasStatus setCanvasType(String canvasType) {
        this.canvasType = canvasType;
        return this;
    }

    public Map<String, Object> getCanvasParams() {
        return canvasParams;
    }

    public CanvasStatus setCanvasParams(Map<String, Object> canvasParams) {
        this.canvasParams = canvasParams;
        return this;
    }

    public String getCanvasOpener() {
        return canvasOpener;
    }

    public CanvasStatus setCanvasOpener(String canvasOpener) {
        this.canvasOpener = canvasOpener;
        return this;
    }

    public String getSubSessionId() {
        return subSessionId;
    }

    public CanvasStatus setSubSessionId(String subSessionId) {
        this.subSessionId = subSessionId;
        return this;
    }
}
