package ai.agent.service.llmCalling;

import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.engine.groupChat.model.definition.LLMConfig;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface LLMClient extends Serializable {
    /**
     * Simple blocking single-prompt call.
     */
    String callLlm(LLMConfig config, String prompt) throws InterruptedException;

    /**
     * Simple blocking single-prompt call.
     */
    String callLlm(LLMConfig config, String systemPrompt, String userPrompt) throws InterruptedException;

    /**
     * Blocking chat-completions call with OpenAI-compatible messages array.
     */
    String callChat(LLMConfig config, List<LlmMessage> messages) throws InterruptedException;
    
    /**
     * Blocking chat-completions call with Function Calling support.
     * 
     * @param config LLM configuration
     * @param messages Chat messages
     * @param tools Available tools for function calling
     * @return LLM response (JSON string containing text and/or tool calls)
     */
    default String callChatWithFunctions(LLMConfig config, List<LlmMessage> messages, List<Map<String, Object>> tools) throws InterruptedException {
        // 默认实现：如果没有工具，回退到普通调用
        if (tools == null || tools.isEmpty()) {
            return callChat(config, messages);
        }
        // 子类应该重写此方法以支持Function Calling
        throw new UnsupportedOperationException("Function Calling not supported by this LLM client");
    }
}

