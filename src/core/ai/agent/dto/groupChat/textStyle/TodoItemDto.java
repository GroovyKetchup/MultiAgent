package ai.agent.dto.groupChat.textStyle;

import ai.agent.enums.TodoStatus;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("待办事项Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-02", updateTime = "2025-12-02"
)
public class TodoItemDto implements Serializable {

    // 序号（从1开始）
    private Integer index;
    // 事项名称
    private String itemName;
    // 事项描述
    private String itemDesc;
    // 状态
    private TodoStatus status;

    public Integer getIndex() {
        return index;
    }

    public TodoItemDto setIndex(Integer index) {
        this.index = index;
        return this;
    }

    public String getItemName() {
        return itemName;
    }

    public TodoItemDto setItemName(String itemName) {
        this.itemName = itemName;
        return this;
    }

    public String getItemDesc() {
        return itemDesc;
    }

    public TodoItemDto setItemDesc(String itemDesc) {
        this.itemDesc = itemDesc;
        return this;
    }

    public TodoStatus getStatus() {
        return status;
    }

    public TodoItemDto setStatus(TodoStatus status) {
        this.status = status;
        return this;
    }
}
