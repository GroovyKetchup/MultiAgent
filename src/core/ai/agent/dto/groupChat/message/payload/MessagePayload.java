package ai.agent.dto.groupChat.message.payload;

import com.alibaba.fastjson2.annotation.JSONField;

import java.io.Serializable;

/**
 * 消息载荷基类
 * 所有消息载荷类型的基类
 */

public abstract class MessagePayload implements Serializable {

    /**
     * 获取载荷类型标识
     *
     * @return 载荷类型字符串
     */
    @JSONField(name = "type", ordinal = -1)
    public abstract String getPayloadType();

    /**
     * 验证载荷数据的有效性
     *
     * @return 是否有效
     */
    public abstract boolean isValid();
}
