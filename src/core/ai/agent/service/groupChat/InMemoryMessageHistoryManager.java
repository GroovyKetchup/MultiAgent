package ai.agent.service.groupChat;

import ai.agent.dto.groupChat.message.Message;
import octocm.domain.dto.DomainDto;

public class InMemoryMessageHistoryManager extends MessageHistoryManager {

    @Override
    public void addMessage(DomainDto domain, Message message) {
        getMessageHistory().offer(message);
        
        while (getMessageHistory().size() > getMaxHistorySize()) {
            getMessageHistory().poll();
        }
    }
}
