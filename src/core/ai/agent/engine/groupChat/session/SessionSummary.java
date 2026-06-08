package ai.agent.engine.groupChat.session;

import java.io.Serializable;

/**
 * 会话轮次压缩总结记录
 */
public class SessionSummary implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final int roundIndex;           // 轮次序号
    private final String summary;           // 压缩后的总结
    private final long timestamp;           // 压缩时间
    private final int messageCount;         // 本轮消息数量

    public SessionSummary(int roundIndex, String summary, long timestamp, int messageCount) {
        this.roundIndex = roundIndex;
        this.summary = summary;
        this.timestamp = timestamp;
        this.messageCount = messageCount;
    }

    public int getRoundIndex() {
        return roundIndex;
    }

    public String getSummary() {
        return summary;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getMessageCount() {
        return messageCount;
    }

    @Override
    public String toString() {
        return "SessionSummary{" +
                "roundIndex=" + roundIndex +
                ", messageCount=" + messageCount +
                ", timestamp=" + timestamp +
                '}';
    }
}
