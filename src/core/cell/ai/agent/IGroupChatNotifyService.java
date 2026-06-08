package cell.ai.agent;


import ai.agent.constant.NotifyConstants;
import bap.cells.Cells;
import cell.octo.cm.tools.util.SmsTencentUtil;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import com.tencentcloudapi.common.Credential;
import org.nutz.dao.entity.annotation.Comment;

@Comment("群聊通知服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-16", updateTime = "2025-09-16"
)
// cell.ai.agent.IGroupChatNotifyService
public interface IGroupChatNotifyService extends IGroupChatBasicService {
    static IGroupChatNotifyService get() {
        return Cells.get(IGroupChatNotifyService.class);
    }

    // ========================= 业务化方法 =========================


    // 发送任务结束的通知
    default void sendTaskOverSmsNotify(String phone) {
        if (StrUtil.isBlank(phone)) return;
        smsNotify(phone, NotifyConstants.SMS_CONTENT_TEMPLATE_TASK_OVER);
    }






    // ========================= 无业务化方法 =========================


    // 短信通知
    default void smsNotify(String phone, String contentTemplateId) {
        if (StrUtil.hasBlank(phone, contentTemplateId)) return;

        try {
            Credential cred = SmsTencentUtil.getCredential(null, null);
            SmsTencentUtil.send(cred, null, null, null, null,
                    contentTemplateId, new String[]{}, phone);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}
