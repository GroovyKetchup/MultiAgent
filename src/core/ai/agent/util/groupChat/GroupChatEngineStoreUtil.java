package ai.agent.util.groupChat;

import ai.agent.dto.groupChat.AgentChatRecordDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.message.payload.*;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.enums.MessageType;
import ai.agent.service.groupChat.MessageHistoryManager;
import ai.agent.util.ConsolePrintUtil;
import cell.ai.agent.IGroupChatMessageService;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import gpf.adur.data.Form;
import octo.cm.util.EasyOperation;
import octocm.domain.dto.DomainDto;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;

import static ai.agent.constant.GroupChatConstants.FormModelId_GroupChatEngineData;

@Comment("群聊引擎归档工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-11-05", updateTime = "2025-11-05"
)
public class GroupChatEngineStoreUtil {

    public static final EasyOperation Op = EasyOperation.get();

    public static GroupChatEngine tryLoadStoredData(String groupChatInstId) {
        if (StrUtil.isBlank(groupChatInstId)) return null;

        try (IDao dao = IDaoService.newIDao()) {


            // 之前有一种情况，群聊编号只是业务域编号
            Form engineDataForm = Op.queryFormByCondition(dao, FormModelId_GroupChatEngineData,
                    "房间编号", groupChatInstId, null);


            if (engineDataForm == null) return null;

            String engineData = engineDataForm.getString("引擎数据");
            if (StrUtil.isBlank(engineData)) return null;


            GroupChatEngine storedBean = JSON.parseObject(engineData, GroupChatEngine.class,
                    JSONReader.Feature.SupportAutoType,
                    JSONReader.Feature.SupportClassForName,
                    JSONReader.Feature.SupportSmartMatch,
                    JSONReader.Feature.IgnoreAutoTypeNotMatch
            );

            MessageHistoryManager messageHistoryManager = doBuildMessageHistoryManager(storedBean);
            storedBean.resetMessageHistoryManager(messageHistoryManager);

            return storedBean;

        } catch (Exception e) {
            return null;
//            throw new RuntimeException(e);
        }

    }

    // 保存所有数据
    public static void saveTotalData(GroupChatEngine groupChatEngine) {

        if (groupChatEngine == null) {
            return;
        }
        GroupChatInstance groupChatInstance = groupChatEngine.getGroupChatInstance();
        if (groupChatInstance == null) {
            return;
        }

        DomainDto busDomain = groupChatEngine.getBusDomain();
        if (busDomain == null) {
            return;
        }
        try (IDao dao = IDaoService.newIDao()) {

            String groupChatInstId = groupChatInstance.getInstanceId();
            Form form = Op.queryFormByCondition(dao, FormModelId_GroupChatEngineData,
                    "房间编号", busDomain.getDomainCode(), null);

            boolean isCreate = false;
            if (form == null) {
                isCreate = true;
                form = Op.newForm(FormModelId_GroupChatEngineData)
                        .setAttrValue("房间编号",
                                busDomain.getDomainCode());
            }

            form.setAttrValue(Form.Owner, busDomain.getDomainUuid());

            form.setAttrValue("引擎数据", JSON.toJSONString(groupChatEngine,
                            JSONWriter.Feature.WriteMapNullValue,
                            JSONWriter.Feature.FieldBased,          // 序列化无 Getter 的私有字段
                            JSONWriter.Feature.WriteEnumUsingToString
                    )
            );

//            ConsolePrintUtil.printRedLn(StrUtil.format("saveTotalData: isCreate is {}", isCreate));


            if (isCreate) {
                IFormMgr.get().createForm(dao, form);

            } else {
                IFormMgr.get().updateForm(dao, form);
            }
            dao.commit();


        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
            throw new RuntimeException(e);
        }
    }


    public static void removeStoredData(IDao dao, String groupChatInstId) {

        try {
            Cnd cnd = Cnd.NEW();
            cnd.where().andEquals(
                    Op.getFieldCode("房间编号"),
                    groupChatInstId
            );
            IFormMgr.get().deleteForm(dao, FormModelId_GroupChatEngineData, cnd);

        } catch (Exception e) {
            Op.logException(e);
        }
    }


    // ========================= 支撑方法 =========================


    private static MessageHistoryManager doBuildMessageHistoryManager(GroupChatEngine engine) {
        MessageHistoryManager messageHistoryManager = new MessageHistoryManager();
        try {
            if (engine == null) throw new RuntimeException("引擎不得为空");

            String busDomainCode = engine.getBusDomain().getDomainCode();
            if (StrUtil.isBlank(busDomainCode)) throw new RuntimeException("业务域不得为空");

            List<AgentChatRecordDto> chatRecordDtos = IGroupChatMessageService.get()
                    .queryAllByBusDomainCode(busDomainCode);
            for (AgentChatRecordDto chatRecordDto : chatRecordDtos) {
                try {
                    String msgContent = chatRecordDto.getMsgContent();
                    JSONObject jsonObject = JSON.parseObject(msgContent);

                    Message parsedMessage = doParseMessage(jsonObject); // 使用我们优化的方法
                    if (parsedMessage != null) {
                        messageHistoryManager.addMessageOnlyHistory( parsedMessage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            return messageHistoryManager;
        } catch (Exception e) {
            e.printStackTrace();
            return messageHistoryManager;
        }
    }


    private static Message doParseMessage(JSONObject messageObj) {
        if (messageObj == null) {
            return null;
        }

        Message msg;
        try {
            msg = messageObj.toJavaObject(Message.class);
        } catch (Exception e) {
            System.err.println("解析 Message 基础字段失败: " + e.getMessage());
            msg = new Message(); // 或者创建一个空的
        }


        JSONObject payloadObj = messageObj.getJSONObject("payload");
        if (payloadObj == null) {
            msg.setPayload(null); // 确保 payload 为 null
            return msg;
        }

        MessageType messageType = msg.getMessageType();
        if (messageType == null) {
            String messageTypeStr = messageObj.getString("messageType");
            if (messageTypeStr == null) {
                System.err.println("无法确定 messageType，payload 解析失败");
                return msg; // 无法解析
            }
            try {
                messageType = MessageType.valueOf(messageTypeStr);
            } catch (IllegalArgumentException e) {
                System.err.println("未知的 MessageType: " + messageTypeStr);
                return msg; // 未知类型
            }
        }


        MessagePayload payload = null;
        try {
            switch (messageType) {
                case TEXT:
                    payload = payloadObj.toJavaObject(TextPayload.class);
                    break;
                case OPERATE:
                    payload = payloadObj.toJavaObject(OperatePayload.class);
                    break;
                case ATTACHMENT:
                    payload = payloadObj.toJavaObject(AttachmentPayload.class);
                    break;
                case PLAN:
                    payload = payloadObj.toJavaObject(PlanPayload.class);
                    break;
                case AGENT_ERROR:
                    payload = payloadObj.toJavaObject(AgentErrorPayload.class);
                    break;
                case TOOL_CALL:
                    payload = payloadObj.toJavaObject(ToolCallPayload.class);
                    break;
                // TODO: 确保所有 MessageType 都有对应的 case
                case SYSTEM:
                    // 假设 System 消息没有 payload 或使用 TextPayload
                    // payload = payloadObj.toJavaObject(SystemPayload.class);
                    break;
                default:
                    System.err.println("未处理的 MessagePayload 类型: " + messageType);
                    break;
            }
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getFullStackTrace(e));
            // 即使 payload 解析失败，也返回 message 对象（只是 payload 为 null）
        }

        msg.setPayload(payload);
        return msg;
    }

    public static void main(String[] args) {
        String str = "{\"attachmentMessage\":false,\"messageType\":2,\"msgId\":\"70eb43da-9f4b-423c-a0ae-7f8d61ca7bf9\",\"operateMessage\":false,\"payload\":{\"type\":\"PLAN\",\"description\":\"我制定了以下原子级执行计划（共2个步骤）：\\n\\n步骤 1: 设置应用名称为智能化OA办公系统\\n步骤 2: 进行任务编制\\n\\n系统将自动执行每个步骤，每个步骤都会获得前面步骤的执行上下文。\",\"plan\":{\"createdTime\":1762422756371,\"currentStep\":{\"attempts\":0,\"description\":\"\",\"endTime\":0,\"executionDuration\":0,\"executionMessages\":[],\"goal\":\"设置应用名称为智能化OA办公系统\",\"id\":\"1\",\"llmResponds\":[],\"startTime\":0,\"status\":0,\"waitingForUserInput\":false},\"currentStepIndex\":0,\"status\":0,\"steps\":[{\"attempts\":0,\"description\":\"\",\"endTime\":0,\"executionDuration\":0,\"executionMessages\":[],\"goal\":\"设置应用名称为智能化OA办公系统\",\"id\":\"1\",\"llmResponds\":[],\"startTime\":0,\"status\":0,\"waitingForUserInput\":false},{\"attempts\":0,\"description\":\"\",\"endTime\":0,\"executionDuration\":0,\"executionMessages\":[],\"goal\":\"进行任务编制\",\"id\":\"2\",\"llmResponds\":[],\"startTime\":0,\"status\":0,\"waitingForUserInput\":false}],\"updateTime\":1762422756371},\"valid\":true},\"planMessage\":true,\"planPayload\":{\"type\":\"PLAN\",\"description\":\"我制定了以下原子级执行计划（共2个步骤）：\\n\\n步骤 1: 设置应用名称为智能化OA办公系统\\n步骤 2: 进行任务编制\\n\\n系统将自动执行每个步骤，每个步骤都会获得前面步骤的执行上下文。\",\"plan\":{\"createdTime\":1762422756371,\"currentStep\":{\"attempts\":0,\"description\":\"\",\"endTime\":0,\"executionDuration\":0,\"executionMessages\":[],\"goal\":\"设置应用名称为智能化OA办公系统\",\"id\":\"1\",\"llmResponds\":[],\"startTime\":0,\"status\":0,\"waitingForUserInput\":false},\"currentStepIndex\":0,\"status\":0,\"steps\":[{\"attempts\":0,\"description\":\"\",\"endTime\":0,\"executionDuration\":0,\"executionMessages\":[],\"goal\":\"设置应用名称为智能化OA办公系统\",\"id\":\"1\",\"llmResponds\":[],\"startTime\":0,\"status\":0,\"waitingForUserInput\":false},{\"attempts\":0,\"description\":\"\",\"endTime\":0,\"executionDuration\":0,\"executionMessages\":[],\"goal\":\"进行任务编制\",\"id\":\"2\",\"llmResponds\":[],\"startTime\":0,\"status\":0,\"waitingForUserInput\":false}],\"updateTime\":1762422756371},\"valid\":true},\"senderId\":\"system_scheduler\",\"systemMessage\":false,\"textMessage\":false,\"timestamp\":1762422756373,\"toolCallMessage\":false,\"valid\":true}";


        Message message = doParseMessage(JSON.parseObject(str));
        System.out.println(JSON.toJSONString(message));
        System.out.println(message.getMessageType());
        System.out.println(message.getPayload().getClass().getName());

    }

}
