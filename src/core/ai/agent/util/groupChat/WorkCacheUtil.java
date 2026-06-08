package ai.agent.util.groupChat;

import ai.agent.dto.groupChat.workCache.WorkCachePanelDesignDto;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import gpf.adur.data.Form;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;

@Comment("工作缓存工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-10-21", updateTime = "2025-10-21"
)
public class WorkCacheUtil {

    // 将面板Form存入缓存
    public static List<WorkCachePanelDesignDto> convertPdsToWorkCachePds(List<Form> panelDesignForms) throws Exception {
        if (CollUtil.isEmpty(panelDesignForms)) return new ArrayList<>();

        List<WorkCachePanelDesignDto> wcPanelDesignDtos = new ArrayList<>();

        for (Form panelDesignForm : panelDesignForms) {

            WorkCachePanelDesignDto dto = WorkCachePanelDesignDto.newDto(panelDesignForm);
            if (dto != null) {
                wcPanelDesignDtos.add(dto);

            }
        }

        return wcPanelDesignDtos;


    }
}
