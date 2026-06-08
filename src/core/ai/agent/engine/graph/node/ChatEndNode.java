package ai.agent.engine.graph.node;

import ai.agent.dto.graph.NodeExecutionResult;
import ai.agent.engine.graph.AbstractGraphNode;
import ai.agent.engine.graph.GraphContext;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import static ai.agent.constant.GraphConstants.NODE_CHAT_END;

@Comment("图-对话结束节点")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-01", updateTime = "2025-12-01"
)
public class ChatEndNode extends AbstractGraphNode {
    @Override
    public String getName() {
        return NODE_CHAT_END;
    }

    @Override
    public NodeExecutionResult execute(GraphContext ctx) {
        return NodeExecutionResult.terminate()
                .withSummary("对话结束");
    }
}
