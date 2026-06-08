package cell.ai.agent;

import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.session.SubSessionDto;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.service.groupChat.manager.GroupChatEngineManager;
import ai.agent.util.ConsolePrintUtil;
import cell.ServiceCellIntf;
import cmn.anotation.ClassDeclare;
import fe.cmn.panel.PanelContext;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Comment("子会话服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-17", updateTime = "2025-12-17"
)
//  cell.ai.agent.ISubSessionFrontEndService
public interface ISubSessionFrontEndService extends ServiceCellIntf {

    // 获取子会话列表
    @SuppressWarnings("unused")
    default RespondDto getSubSessionList(PanelContext panelContext, String parentSessionId) {
        try {
            GroupChatEngine engine = GroupChatEngineManager.getGroupChatEngine(parentSessionId);
            if (engine == null) {
                return RespondDto.newError("主会话不存在: " + parentSessionId);
            }

            List<SubSessionDto> list = engine.getSubSessionManager().getSubSessionDtosByParent(parentSessionId);
            return RespondDto.newSuccess("获取成功", list);
        } catch (Exception e) {
            return RespondDto.newError("获取子会话列表失败: " + e.getMessage());
        }
    }

    // 获取子会话详情
    @SuppressWarnings("unused")
    default RespondDto getSubSessionDetail(PanelContext panelContext, String subSessionId) {
        try {
            SubSessionInstance subSession = findSubSession(subSessionId);
            if (subSession == null) {
                return RespondDto.newError("子会话不存在: " + subSessionId);
            }

            SubSessionDto dto = SubSessionDto.fromInstance(subSession);
            return RespondDto.newSuccess("获取成功", dto);
        } catch (Exception e) {
            return RespondDto.newError("获取子会话详情失败: " + e.getMessage());
        }
    }

    // 获取子会话消息历史
    @SuppressWarnings("unused")
    default RespondDto getSubSessionMessages(PanelContext panelContext, String subSessionId) {
        try {
            SubSessionInstance subSession = findSubSession(subSessionId);
            if (subSession == null) {
                return RespondDto.newError("子会话不存在: " + subSessionId);
            }

            List<Message> messages = new ArrayList<>();
            if (subSession.getMessageHistoryManager() != null) {
                messages = subSession.getMessageHistoryManager().getFullHistory();
            }

            return RespondDto.newSuccess("获取成功", messages);
        } catch (Exception e) {
            return RespondDto.newError("获取子会话消息失败: " + e.getMessage());
        }
    }

    // 回复子会话
    @SuppressWarnings("unused")
    default RespondDto replyToSubSession(PanelContext panelContext, String subSessionId, String reply) {
        try {
            GroupChatEngine parentEngine = findParentEngine(subSessionId);
            if (parentEngine == null) {
                return RespondDto.newError("子会话不存在: " + subSessionId);
            }

            parentEngine.getSubSessionManager().replyToSubSession(subSessionId, reply);
            return RespondDto.newSuccess("回复成功", null);
        } catch (Exception e) {
            return RespondDto.newError("回复子会话失败: " + e.getMessage());
        }
    }

    // 取消子会话
    @SuppressWarnings("unused")
    default RespondDto cancelSubSession(PanelContext panelContext, String subSessionId) {
        try {
            GroupChatEngine parentEngine = findParentEngine(subSessionId);
            if (parentEngine == null) {
                return RespondDto.newError("子会话不存在: " + subSessionId);
            }

            parentEngine.getSubSessionManager().failSubSession(subSessionId, "用户取消");
            return RespondDto.newSuccess("取消成功", null);
        } catch (Exception e) {
            return RespondDto.newError("取消子会话失败: " + e.getMessage());
        }
    }

    // 打断子会话执行（不终止会话，只是中断当前执行并等待用户下一步指令）
    @SuppressWarnings("unused")
    default RespondDto interruptSubSession(PanelContext panelContext, String subSessionId) {
        try {
            GroupChatEngine parentEngine = findParentEngine(subSessionId);
            if (parentEngine == null) {
                return RespondDto.newError("子会话不存在: " + subSessionId);
            }

            parentEngine.getSubSessionManager().interruptSubSession(subSessionId);
            return RespondDto.newSuccess("打断成功", null);
        } catch (Exception e) {
            return RespondDto.newError("打断子会话失败: " + e.getMessage());
        }
    }

    // 完成子会话
    @SuppressWarnings("unused")
    default RespondDto completeSubSession(PanelContext panelContext, String subSessionId, String result) {
        try {
            GroupChatEngine parentEngine = findParentEngine(subSessionId);
            if (parentEngine == null) {
                return RespondDto.newError("子会话不存在: " + subSessionId);
            }

            parentEngine.getSubSessionManager().completeSubSession(subSessionId, result);
            return RespondDto.newSuccess("完成成功", null);
        } catch (Exception e) {
            return RespondDto.newError("完成子会话失败: " + e.getMessage());
        }
    }

    // 移除子会话
    @SuppressWarnings("unused")
    default RespondDto removeSubSession(PanelContext panelContext, String subSessionId) {
        try {
            GroupChatEngine parentEngine = findParentEngine(subSessionId);
            if (parentEngine == null) {
                return RespondDto.newError("子会话不存在: " + subSessionId);
            }

            parentEngine.getSubSessionManager().removeSubSession(subSessionId);
            return RespondDto.newSuccess("移除成功", null);
        } catch (Exception e) {
            return RespondDto.newError("移除子会话失败: " + e.getMessage());
        }
    }

    // 获取子会话画布状态
    @SuppressWarnings("unused")
    default RespondDto getSubSessionCanvasStatus(PanelContext panelContext, String subSessionId) {
        try {
            SubSessionInstance subSession = findSubSession(subSessionId);
            if (subSession == null) {
                return RespondDto.newError("子会话不存在: " + subSessionId);
            }

            return RespondDto.newSuccess("获取成功", subSession.getCanvasStatus());
        } catch (Exception e) {
            return RespondDto.newError("获取子会话画布状态失败: " + e.getMessage());
        }
    }

    // 注册子会话画布动作
    @SuppressWarnings("unused")
    default RespondDto registerSubSessionCanvas(
            PanelContext panelContext,
            String subSessionId,
            LinkedHashMap currentCanvasStatusObj,
            String operationGuidance,
            List<LinkedHashMap> actionObjs) {
        try {
            SubSessionInstance subSession = findSubSession(subSessionId);
            if (subSession == null) {
                return RespondDto.newError("子会话不存在: " + subSessionId);
            }

            GroupChatEngine subEngine = subSession.getGroupChatEngine();
            if (subEngine == null) {
                return RespondDto.newError("子会话引擎未初始化");
            }

            ai.agent.dto.groupChat.canvas.CanvasStatus currentCanvasStatus =
                    safeJsonToBean(currentCanvasStatusObj, ai.agent.dto.groupChat.canvas.CanvasStatus.class);

            currentCanvasStatus.setSubSessionId(subSessionId);

            List<ai.agent.dto.frontendCalling.FrontendActionDto> actions = new java.util.ArrayList<>();
            if (actionObjs != null && !actionObjs.isEmpty()) {
                for (LinkedHashMap actionObj : actionObjs) {
                    ai.agent.dto.frontendCalling.FrontendActionDto action =
                            safeJsonToBean(actionObj, ai.agent.dto.frontendCalling.FrontendActionDto.class);
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }

            ConsolePrintUtil.printGreenLn(
                    cn.hutool.core.util.StrUtil.format(
                            "[子会话画布注册] subSessionId:{}, canvasType:{}, actions.size:{}",
                            subSessionId,
                            currentCanvasStatus.getCanvasType(),
                            actions.size()
                    )
            );

            subEngine.getFrontendActionManager()
                    .register(currentCanvasStatus, operationGuidance, actions, true);

            subSession.setCanvasStatus(currentCanvasStatus);

            return RespondDto.newSuccess("注册成功", null);
        } catch (Exception e) {
            return RespondDto.newError("注册失败: " + e.getMessage());
        }
    }


    // ========================= 支撑方法 =========================

    default <T> T safeJsonToBean(Object object, Class<T> beanClass) {
        try {
            return cn.hutool.json.JSONUtil.toBean(
                    cn.hutool.json.JSONUtil.toJsonStr(object),
                    beanClass
            );
        } catch (Exception e) {
            ai.agent.util.ConsolePrintUtil.printRedLn(
                    cn.hutool.core.util.StrUtil.format(
                            "转换失败(safeJsonToBean), object:\n{}\n, err:\n{}",
                            cn.hutool.json.JSONUtil.toJsonStr(object),
                            org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e)
                    )
            );
            return null;
        }
    }

    // 获取子会话
    default SubSessionInstance findSubSession(String subSessionId) {
        for (GroupChatEngine engine : GroupChatEngineManager.getAllEngines()) {
            SubSessionInstance subSession = engine.getSubSessionManager().getSubSession(subSessionId);
            if (subSession != null) {
                return subSession;
            }
        }
        return null;
    }

    // 获取父会话
    default GroupChatEngine findParentEngine(String subSessionId) {
        for (GroupChatEngine engine : GroupChatEngineManager.getAllEngines()) {
            SubSessionInstance subSession = engine.getSubSessionManager().getSubSession(subSessionId);
            if (subSession != null) {
                return engine;
            }
        }
        return null;
    }
}
