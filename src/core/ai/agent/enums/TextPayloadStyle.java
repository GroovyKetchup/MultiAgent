package ai.agent.enums;

public enum TextPayloadStyle {
    NORMAL("normal"),
    HTML("html"),
    THINKING("thinking"),
    HINT("hint"),
    TODO("todo"),
    OMITTED("omitted"),
    INTERACTIVE("interactive"),
    ERROR("error"),
    TASK_EXECUTION("task_execution")
    ;

    private String value;

    TextPayloadStyle(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public TextPayloadStyle setValue(String value) {
        this.value = value;
        return this;
    }


    @Override
    public String toString() {
        return value;
    }
}
