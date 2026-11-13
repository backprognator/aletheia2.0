package aletheia.wire;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Reads completion chunks, full completions, and OpenRouter error bodies. */
public final class CompletionParser {
    private CompletionParser() {}

    public static Piece parse(String payload) {
        if (payload == null) {
            return Piece.text("");
        }
        String trimmed = payload.trim();
        if (trimmed.isEmpty()) {
            return Piece.text("");
        }
        if ("[DONE]".equals(trimmed)) {
            return Piece.done();
        }
        try {
            JSONObject root = new JSONObject(trimmed);
            if (root.has("error") && !root.isNull("error")) {
                String message = errorMessageOf(root.optJSONObject("error"));
                if (message != null) {
                    return Piece.error(message);
                }
            }
            JSONArray choices = root.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return Piece.text("");
            }
            JSONObject choice = choices.optJSONObject(0);
            if (choice == null) {
                return Piece.text("");
            }
            if (choice.has("error") && !choice.isNull("error")) {
                String message = errorMessageOf(choice.optJSONObject("error"));
                if (message != null) {
                    return Piece.error(message);
                }
            }
            JSONObject delta = choice.optJSONObject("delta");
            if (delta != null && delta.has("content")) {
                return Piece.text(contentToString(delta.get("content")));
            }
            JSONObject message = choice.optJSONObject("message");
            if (message != null && message.has("content")) {
                return Piece.text(contentToString(message.get("content")));
            }
            if (choice.has("text") && !choice.isNull("text")) {
                return Piece.text(choice.getString("text"));
            }
            return Piece.text("");
        } catch (JSONException exception) {
            return Piece.error("Could not read the model response.");
        }
    }

    public static String errorMessage(int status, String body) {
        if (body != null && !body.isBlank()) {
            try {
                JSONObject root = new JSONObject(body);
                if (root.has("error") && !root.isNull("error")) {
                    Object error = root.get("error");
                    if (error instanceof JSONObject) {
                        String message = errorMessageOf((JSONObject) error);
                        if (message != null) {
                            return message;
                        }
                    }
                    if (error instanceof String && !((String) error).isBlank()) {
                        return (String) error;
                    }
                }
                if (root.has("message") && !root.isNull("message")) {
                    return root.getString("message");
                }
            } catch (JSONException ignored) {
                // Fall through to the status line.
            }
        }
        return "OpenRouter returned " + status;
    }

    private static String errorMessageOf(JSONObject error) throws JSONException {
        if (error == null || !error.has("message") || error.isNull("message")) {
            return null;
        }
        String message = error.getString("message");
        return message.isBlank() ? null : message;
    }

    private static String contentToString(Object content) throws JSONException {
        if (content == null || content == JSONObject.NULL) {
            return "";
        }
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof JSONArray) {
            StringBuilder builder = new StringBuilder();
            JSONArray parts = (JSONArray) content;
            for (int i = 0; i < parts.length(); i++) {
                JSONObject part = parts.optJSONObject(i);
                if (part != null && part.has("text") && !part.isNull("text")) {
                    builder.append(part.getString("text"));
                }
            }
            return builder.toString();
        }
        return "";
    }
}
