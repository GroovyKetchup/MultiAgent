package ai.agent.engine.graph.node;

import ai.agent.dto.graph.NodeExecutionResult;
import ai.agent.engine.graph.AbstractGraphNode;
import ai.agent.engine.graph.GraphContext;
import ai.agent.engine.groupChat.GroupChatEngine;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import static ai.agent.constant.GraphConstants.NODE_CHAT_BEGIN;
import static ai.agent.constant.GraphConstants.NODE_ROUTER;

@Comment("图-对话开始节点")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-01", updateTime = "2025-12-01"
)
public class ChatBeginNode extends AbstractGraphNode {
    @Override
    public String getName() {
        return NODE_CHAT_BEGIN;
    }

    @Override
    public NodeExecutionResult execute(GraphContext ctx) {
        GroupChatEngine chatEngine = ctx.getChatEngine();

        return NodeExecutionResult.toNode(NODE_ROUTER)
                .withSummary("对话开始，进入路由节点");
    }
}
