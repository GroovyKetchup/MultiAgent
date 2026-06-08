package ai.agent.util;

import cell.octo.cm.basic.http.IBasicAppWebService;
import cmn.anotation.ClassDeclare;
import fe.cmn.panel.PanelContext;
import gpf.dc.http.AppUserInfo;
import org.nutz.dao.entity.annotation.Comment;

import java.util.Map;

@Comment("用户信息工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-11-06", updateTime = "2025-11-06"
)
public class UserInfoUtil {

    // 解析当前的用户信息
    public static AppUserInfo parseUserInfo(PanelContext panelContext) {
        if (panelContext == null) return null;
        Map<String, Object> objectMap = panelContext.getQueryParameters();
        Object tokenObj = null;
        if (objectMap == null || objectMap.isEmpty() ||
                !((tokenObj = objectMap.get("token")) instanceof String)) return null;

        try {
            return IBasicAppWebService.get().parseToken(String.valueOf(tokenObj));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
