package aletheia.wire;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/** Builds the JSON body for POST /chat/completions. */
public final class CompletionRequest {
    private CompletionRequest() {}

    public static String body(String model, List<ChatTurn> turns, boolean stream) {
        JSONObject root = new JSONObject();
        if (model != null && !model.isBlank()) {
            root.put("model", model.trim());
        }
        root.put("stream", stream);
        JSONArray messages = new JSONArray();
        if (turns != null) {
            for (ChatTurn turn : turns) {
                JSONObject message = new JSONObject();
                message.put("role", turn.role);
                message.put("content", turn.content);
                messages.put(message);
            }
        }
        root.put("messages", messages);
        return root.toString();
    }
}
