package ai.agent.dto.groupChat.textStyle;

import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import octo.cm.dto.ErrorDto;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Comment("错误信息项Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-01-08", updateTime = "2025-01-08"
)
public class ErrorItemDto implements Serializable {

    // 序号（从1开始）
    private Integer index;
    // 错误名称
    private String errorName;
    // 错误详情
    private String errorDesc;

    // ========================= 支撑方法 =========================

    // 将发布面板的报错转换为文本样式需要的那个Dto
    public static List<ErrorItemDto> fromPanelDesignPublishError(List<ErrorDto> errors) {
        if (CollUtil.isEmpty(errors)) return Collections.emptyList();

        AtomicInteger idx = new AtomicInteger(1);
        return CollStreamUtil.toList(errors, error -> {
            ErrorItemDto dto = new ErrorItemDto();
            dto.setErrorNameWithMaxLength(error.getErrorKey());
            dto.setErrorDesc(error.getErrorContent());
            dto.setIndex(idx.getAndIncrement());
            return dto;
        });


    }


    // ========================= getter/setter =========================

    public Integer getIndex() {
        return index;
    }

    public ErrorItemDto setIndex(Integer index) {
        this.index = index;
        return this;
    }

    public String getErrorName() {
        return errorName;
    }

    public ErrorItemDto setErrorName(String errorName) {
        this.errorName = errorName;
        return this;
    }

    public ErrorItemDto setErrorNameWithMaxLength(String errorName) {
        if (StrUtil.isNotBlank(errorName) && errorName.length() > 80) {
            this.errorName = errorName.substring(0, 80) + "...";
        } else {
            this.errorName = errorName;
        }
        return this;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public ErrorItemDto setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
        return this;
    }
}
