package ai.agent.service.frontendCalling;

import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.IdUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Comment("前端调用管理类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-11-20", updateTime = "2025-11-20"
)
public class FrontendAsyncCallService {

    // 等待中的方法调用
    public static final Map<String, CompletableFuture<Object>> pendingCalls =
            new ConcurrentHashMap<>();

    // TODO 对于大于60秒的任务应该变成异步任务，同时也要思考智能体如何设计一个等待机制
    public static final int DEFAULT_TIMEOUT_TIME_SECONDS = 120;


    // 调用前端方法
    public static Object callAction(GroupChatEngine engine,
                                    String actionName, Map<String, Object> actionParams, long timeout) throws Exception {
        return callAction(engine, actionName, actionParams, timeout, null);
    }

    // 调用前端方法
    // FIXME 当前仅在Canvas中使用，后面可以重新进行封装
    public static Object callAction(GroupChatEngine engine,
                                    String actionName, Map<String, Object> actionParams, long timeout, String subSessionId) throws Exception {
        String requestId = IdUtil.fastSimpleUUID();
        CompletableFuture<Object> future = new CompletableFuture<>();

        pendingCalls.put(requestId, future);

        // 下发指令
        GroupChatMessageSender.Canvas.callAction(engine, requestId, actionName, actionParams, subSessionId);

        try {
            if (timeout <= 0) timeout = DEFAULT_TIMEOUT_TIME_SECONDS;
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new TimeoutException("Function call to frontend timed out after 10 seconds. Request ID: " + requestId);
        } finally {
            pendingCalls.remove(requestId);
        }


    }


    // 提交前端的响应
    public static void submitResponse(String requestId, Object result) {
        CompletableFuture<Object> future = pendingCalls.get(requestId);

        if (future != null) {
            future.complete(result);
        } else {
            // 可能是响应延迟，callAction 线程已超时并清理了 Map，此处忽略即可。
            ConsolePrintUtil.printRedLn(
                    "FrontendAsyncCallService: 响应延迟，callAction 线程已超时并清理了 Map，此处忽略即可。" +
                            "Request ID: " + requestId
            );
        }
    }
}
