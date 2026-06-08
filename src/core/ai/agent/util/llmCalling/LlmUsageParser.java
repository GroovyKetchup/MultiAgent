package ai.agent.util.llmCalling;

import ai.agent.dto.llmCalling.LlmUsageStats;
import ai.agent.util.ConsolePrintUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Map;

/**
 * LLM使用统计解析器
 * 支持多厂商响应格式解析，统一输出到LlmUsageStats
 * 
 * 解析优先级：
 * 1. OpenAI格式（prompt_tokens_details.cached_tokens）
 * 2. DeepSeek格式（prompt_cache_hit_tokens / prompt_cache_miss_tokens）
 * 3. Anthropic格式（cache_read_input_tokens / cache_creation_input_tokens）
 */
public class LlmUsageParser {

    /**
     * 从LLM响应中解析使用统计
     * 
     * @param responseMap 解析后的响应Map
     * @return LlmUsageStats 使用统计，如果无法解析返回null
     */
    public static LlmUsageStats parseUsage(Map<String, Object> responseMap) {
        if (responseMap == null || !responseMap.containsKey("usage")) {
            return null;
        }

        try {
            Object usageObj = responseMap.get("usage");
            if (!(usageObj instanceof Map)) {
                return null;
            }

            Map<String, Object> usageMap = (Map<String, Object>) usageObj;
            LlmUsageStats stats = new LlmUsageStats();

            // 解析基础Token统计（通用格式）
            int promptTokens = getIntValue(usageMap, "prompt_tokens");
            int completionTokens = getIntValue(usageMap, "completion_tokens");
            int totalTokens = getIntValue(usageMap, "total_tokens");
            
            stats.setPromptTokens(promptTokens);
            stats.setCompletionTokens(completionTokens);
            stats.setTotalTokens(totalTokens);

            // 解析缓存统计（按优先级尝试）
            parseCacheStats(usageMap, stats, promptTokens);

            return stats;

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[LlmUsageParser] 解析usage失败: {}", e.getMessage()));
            return null;
        }
    }

    /**
     * 解析缓存统计（按优先级尝试各厂商格式）
     */
    private static void parseCacheStats(Map<String, Object> usageMap, LlmUsageStats stats, int promptTokens) {
        // 1. 优先尝试OpenAI格式：prompt_tokens_details.cached_tokens
        if (usageMap.containsKey("prompt_tokens_details")) {
            Object detailsObj = usageMap.get("prompt_tokens_details");
            if (detailsObj instanceof Map) {
                Map<String, Object> details = (Map<String, Object>) detailsObj;
                int cachedTokens = getIntValue(details, "cached_tokens");
                if (cachedTokens > 0 || details.containsKey("cached_tokens")) {
                    stats.setCacheHitTokens(cachedTokens);
                    stats.setCacheMissTokens(promptTokens - cachedTokens);
                    return;
                }
            }
        }

        // 2. 尝试DeepSeek格式：prompt_cache_hit_tokens / prompt_cache_miss_tokens
        int deepseekHit = getIntValue(usageMap, "prompt_cache_hit_tokens");
        int deepseekMiss = getIntValue(usageMap, "prompt_cache_miss_tokens");
        if (deepseekHit > 0 || deepseekMiss > 0 
            || usageMap.containsKey("prompt_cache_hit_tokens") 
            || usageMap.containsKey("prompt_cache_miss_tokens")) {
            stats.setCacheHitTokens(deepseekHit);
            stats.setCacheMissTokens(deepseekMiss);
            return;
        }

        // 3. 尝试Anthropic格式：cache_read_input_tokens / cache_creation_input_tokens
        int anthropicRead = getIntValue(usageMap, "cache_read_input_tokens");
        int anthropicCreation = getIntValue(usageMap, "cache_creation_input_tokens");
        if (anthropicRead > 0 || anthropicCreation > 0
            || usageMap.containsKey("cache_read_input_tokens")
            || usageMap.containsKey("cache_creation_input_tokens")) {
            stats.setCacheHitTokens(anthropicRead);
            stats.setCacheMissTokens(anthropicCreation);
        }
    }

    /**
     * 打印缓存统计日志
     */
    private static void printCacheLog(LlmUsageStats stats) {
        if (!stats.hasCacheStats()) {
            return;
        }
        
        double hitRate = stats.getCacheHitRate();
        if (hitRate < 0) {
            return;
        }
    }

    /**
     * 从Map中安全获取int值
     */
    private static int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
