package ai.agent.engine.groupChat.session;

public enum SubSessionMode {
    SYNC,
    ASYNC;

    public static SubSessionMode fromString(String mode) {
        if (SYNC.name().equalsIgnoreCase(mode)) {
            return SYNC;
        }
        return ASYNC;
    }
}
