package cell.ai.agent;

import ai.agent.service.GroupChatThreadPollManager;
import ai.agent.service.groupChat.manager.GroupChatEngineManager;
import ai.agent.util.ConsolePrintUtil;
import bap.cells.BasicServiceCell;
import bap.cells.SimpleServiceCell;
import cmn.anotation.ClassDeclare;
import cmn.util.Tracer;
import cn.hutool.core.util.StrUtil;
import org.nutz.dao.entity.annotation.Comment;

@Comment("")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-18", updateTime = "2025-09-18"
)
public class CGroupChatEngineServiceCell extends BasicServiceCell implements IGroupChatEngineServiceCell {

    @Override
    protected void doStartService() throws Exception {

        ConsolePrintUtil.printGreenLn(
                StrUtil.format("GroupChatEngineServiceCell: inited.")
        );
    }

    @Override
    protected void doStopService() {

        ConsolePrintUtil.printRedLn(
                StrUtil.format("GroupChatEngineServiceCell: doStopService 被调用, 当前ChatEngine数量: {}", GroupChatEngineManager.size())
        );

        // 停止所有群聊引擎
        GroupChatEngineManager.stopAllChatEngine();
        // 停止所有线程池
        GroupChatThreadPollManager.shutdown();

        ConsolePrintUtil.printRedLn(
                StrUtil.format("GroupChatEngineServiceCell: doStopService 完成, 停止后ChatEngine数量: {}", GroupChatEngineManager.size())
        );
    }

    @Override
    public void log() {
    }
}
