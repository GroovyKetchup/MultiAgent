package cell.ai.agent.http;

import ai.agent.constant.HttpConstants;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import org.nutz.dao.entity.annotation.Comment;

@Comment("群聊文件服务-分发处理配置")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-07-22", updateTime = "2025-07-22"
)
public class FileServerDispatcherMappingBuilder extends fileserver.http.FileServerDispatcherMappingBuilder {
    @Override
    public String[] getIncludePatterns() {
        return new String[]{
                StrUtil.format("{}/**", HttpConstants.RequestUrlPrefix_FileServer),
                StrUtil.format("{}/**", HttpConstants.RequestUrlPrefix_Admin),
                StrUtil.format("{}/**", HttpConstants.RequestUrlPrefix_Evolve)
        };
    }
}
