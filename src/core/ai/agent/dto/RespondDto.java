package ai.agent.dto;

import cmn.anotation.ClassDeclare;
import cn.hutool.json.JSONUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("响应前端的dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-02", updateTime = "2025-09-02"
)
public class RespondDto<T> implements Serializable {

    // 是否成功
    private Boolean isSuccess;
    // 返回消息
    private String msg;
    // 返回数据
    private T data;

    // 快速创建成功的类型
    public static <T> RespondDto<T> newSuccess(String msg, T data) {
        return new RespondDto<T>().setSuccess(true)
                .setMsg(msg)
                .setData(data);
    }

    // 快速创建成功类型
    public static String newStrSuccess(String msg, Object data) {
        return newSuccess(msg, data).toString();
    }

    // 快速创建错误类型
    public static RespondDto<Object> newError(String msg) {
        return new RespondDto<>().setSuccess(false)
                .setMsg(msg);
    }

    // 快速创建错误类型
    public static String newStrError(String msg) {
        return newError(msg).toString();
    }

    public Boolean getSuccess() {
        return isSuccess;
    }

    public RespondDto<T> setSuccess(Boolean success) {
        isSuccess = success;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public RespondDto<T> setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public T getData() {
        return data;
    }

    public RespondDto<T> setData(T data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return JSONUtil.toJsonStr(this);
    }
}
