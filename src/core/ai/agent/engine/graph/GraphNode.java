package ai.agent.engine.graph;


import ai.agent.dto.graph.NodeExecutionResult;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("图-节点")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-01", updateTime = "2025-12-01"
)
public interface GraphNode extends Serializable {

    // 节点名称
    String getName();

    // 执行方法
    NodeExecutionResult execute(GraphContext ctx) throws Exception;


}
