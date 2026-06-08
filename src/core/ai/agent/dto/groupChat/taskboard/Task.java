package ai.agent.dto.groupChat.taskboard;

/**
 * 任务实体（内存版）
 * - 用于演示任务看板相关工具
 */
public class Task {
    public enum Status { TODO, DOING, DONE }

    private final String id;
    private String title;
    private String description;
    private Status status;

    public Task(String id, String title, String description, Status status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(Status status) { this.status = status; }
}

