package ai.agent.dto.llmCalling;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("工具Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-03", updateTime = "2025-09-03"
)
public class ToolDto implements Serializable {
    private String name;
    private String cnName;
    private String description;
    private String classPath;

    public ToolDto() {
    }

    public ToolDto(String name, String cnName, String description) {
        this.name = name;
        this.cnName = cnName;
        this.description = description;
    }


//    public static ToolDto fromByClasses(Class<? extends Tool> clazz){
//        if(clazz == null) return null;
//        try {
//            Tool tool = (Tool)  clazz.getClass().getDeclaredConstructor().newInstance();
//            return tool.convertToDto();
//
//        } catch (Exception ignored) {
//        }
//
//    }



    public String getName() {
        return name;
    }

    public ToolDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getCnName() {
        return cnName;
    }

    public ToolDto setCnName(String cnName) {
        this.cnName = cnName;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ToolDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getClassPath() {
        return classPath;
    }

    public ToolDto setClassPath(String classPath) {
        this.classPath = classPath;
        return this;
    }
}
