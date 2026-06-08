package ai.agent.dto.groupChat.taskboard;

public enum TaskExecutionType {

    STANDARD_DELIVERY("STANDARD_DELIVERY","标准交付" ),
    CUSTOM_DELIVERY("CUSTOM_DELIVERY","自定义交付" );

    private final String value;
    private final String description;

    TaskExecutionType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
