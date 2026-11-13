package aletheia.wire;

/** One message in an OpenRouter chat completion. */
public final class ChatTurn {
    public final String role;
    public final String content;

    public ChatTurn(String role, String content) {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
        this.role = role;
        this.content = content;
    }
}
