package cell.ai.agent;

import ai.agent.service.GroupChatThreadPollManager;
import ai.agent.service.groupChat.manager.GroupChatEngineManager;
import ai.agent.util.ConsolePrintUtil;
import bap.cells.SimpleServiceCell;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import org.nutz.dao.entity.annotation.Comment;

@Comment("")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-18", updateTime = "2025-09-18"
)
public class CGroupChatEngineServiceCell extends SimpleServiceCell implements IGroupChatEngineServiceCell {

    @Override
    protected void doStartService() throws Exception {
        ConsolePrintUtil.printGreenLn(
                StrUtil.format("GroupChatEngineServiceCell: inited.")
        );
    }

    @Override
    protected void doStopService() {
        ConsolePrintUtil.printRedLn(
                StrUtil.format("GroupChatEngineServiceCell: 准备停止服务，当前ChatEngine数量:" + GroupChatEngineManager.size())
        );

        // 停止所有群聊引擎
        GroupChatEngineManager.stopAllChatEngine();
        // 停止所有线程池
        GroupChatThreadPollManager.shutdown();
    }

    @Override
    public void log() {

    }
}
