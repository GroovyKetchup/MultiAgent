package ai.agent.dto.llmCalling;

import java.io.Serializable;

/**
 * LLM调用使用统计
 * 统一的参数结构，厂商差异在LlmUsageParser中隔离处理
 */
public class LlmUsageStats implements Serializable {

    // ==================== 基础Token统计 ====================
    
    /** 输入token数 */
    private int promptTokens;
    
    /** 输出token数 */
    private int completionTokens;
    
    /** 总token数 */
    private int totalTokens;

    // ==================== 缓存统计（统一字段） ====================
    
    /** 缓存命中的token数 */
    private int cacheHitTokens;
    
    /** 缓存未命中的token数 */
    private int cacheMissTokens;

    // ==================== 计算属性 ====================

    /**
     * 获取缓存命中率
     * @return 缓存命中率（0.0 - 1.0），如果无法计算返回 -1
     */
    public double getCacheHitRate() {
        int totalInput = cacheHitTokens + cacheMissTokens;
        if (totalInput <= 0) {
            return -1;
        }
        return (double) cacheHitTokens / totalInput;
    }

    /**
     * 是否有缓存统计信息
     */
    public boolean hasCacheStats() {
        return cacheHitTokens > 0 || cacheMissTokens > 0;
    }

    /**
     * 获取格式化的统计摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Tokens[输入:%d, 输出:%d, 总计:%d]",
            promptTokens, completionTokens, totalTokens));
        
        if (hasCacheStats()) {
            double hitRate = getCacheHitRate();
            if (hitRate >= 0) {
                sb.append(String.format(" 缓存[命中:%d, 未命中:%d, 命中率:%.1f%%]",
                    cacheHitTokens, cacheMissTokens, hitRate * 100));
            }
        }
        
        return sb.toString();
    }

    // ==================== Getter/Setter ====================

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public int getCacheHitTokens() {
        return cacheHitTokens;
    }

    public void setCacheHitTokens(int cacheHitTokens) {
        this.cacheHitTokens = cacheHitTokens;
    }

    public int getCacheMissTokens() {
        return cacheMissTokens;
    }

    public void setCacheMissTokens(int cacheMissTokens) {
        this.cacheMissTokens = cacheMissTokens;
    }
}
