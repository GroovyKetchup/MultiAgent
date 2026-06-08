package ai.agent.enums;

public enum InteractionType {
    OPTIONS("options", "选项列表"),
    CONFIRM("confirm", "确认对话框"),
    RATING("rating", "评分"),
    YES_NO("yes_no", "是否选择");

    private String value;
    private String label;

    InteractionType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return value;
    }
}
