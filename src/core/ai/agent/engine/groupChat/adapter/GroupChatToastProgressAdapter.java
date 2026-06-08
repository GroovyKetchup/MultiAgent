
package ai.agent.engine.groupChat.adapter;

import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cmn.dto.Progress;
import cn.hutool.core.util.StrUtil;
import com.leavay.common.util.ProgressCtrl.crpc.IProgress;
import crpc.BasicCallback;
import octo.cm.constant.WorkBenchConst;

public class GroupChatToastProgressAdapter extends BasicCallback implements IProgress<Object> {
    private static final long serialVersionUID = 7326938955201827425L;

    // 进度Id
    private final String progressId;

    // 群聊引擎
    private final GroupChatEngine engine;

    // 仅工作台消息
    private final Boolean isOnlyShowWorkbenchMessage;

    private GroupChatToastProgressAdapter(GroupChatEngine engine, String progressId) {
        this.engine = engine;
        this.progressId = progressId;
        this.isOnlyShowWorkbenchMessage = true;

    }

    public GroupChatToastProgressAdapter(GroupChatEngine engine, String progressId, Boolean isOnlyShowWorkbenchMessage) {
        this.progressId = progressId;
        this.engine = engine;
        this.isOnlyShowWorkbenchMessage = isOnlyShowWorkbenchMessage;
    }

    public static Progress newProgress(GroupChatEngine chatEngine, String progressId) {
        return new Progress(new GroupChatToastProgressAdapter(chatEngine, progressId));
    }

    public static Progress newProgress(GroupChatEngine chatEngine, String progressId, Boolean isOnlyShowWorkbenchMessage) {
        return new Progress(new GroupChatToastProgressAdapter(chatEngine, progressId, isOnlyShowWorkbenchMessage));
    }

    public void addMessage(String msg) {
        if (StrUtil.isBlank(msg)) return;
        if (isOnlyShowWorkbenchMessage &&
                !msg.startsWith(WorkBenchConst.Progress_FLAG_WorkBench)) {
            return;
        }
        msg = msg.replace(WorkBenchConst.Progress_FLAG_WorkBench, "");
        msg = msg.trim();
        ConsolePrintUtil.printGreenLn(
                StrUtil.format("GroupChatToastProgressAdapter 收到消息[{}]", msg)
        );

        GroupChatMessageSender.Progress.addMsg(engine, progressId, msg);

    }


    @Override
    public int getMaximum() {
        return 100;
    }

    @Override
    public int getMinimum() {
        return 0;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void sendProcess(int process, String msg, boolean blNewLine) {
        this.sendProcess(process, msg, blNewLine, (Object) null);
    }

    @Override
    public void sendProcess(int process, String msg, boolean blNewLine, Object userObject) {
        this.addMessage(msg);
    }

    @Override
    public void sendStopProcess() {
        this.addMessage("Stop Progress");
    }

    @Override
    public void setMessage(String sMsg, boolean blNewLine) {
        this.addMessage(sMsg);
    }

    @Override
    public void setMaximum(int value) {
    }

    @Override
    public void setMinimum(int value) {
    }

    @Override
    public int showConfirmDialog(String msg, String title, int operation) {
        this.addMessage(msg);
        return 0;
    }

    @Override
    public int showConfirmDialog(String msg, String title, int optionType, int messageType) {
        this.addMessage(msg);
        return 0;
    }

    @Override
    public void showMessageDialog(String msg, String title, int messageType) {
        this.addMessage(msg);
    }

    @Override
    public void showMessageDialog(String msg, String title) throws Exception {
        this.addMessage(msg);
    }


    @Override
    public void sendDataFrame(Object data) {
        this.addMessage("Recieve Data : " + data);
    }


}
