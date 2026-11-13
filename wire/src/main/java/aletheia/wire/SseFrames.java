package aletheia.wire;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits an OpenRouter event stream into data payloads.
 * Comment lines (they start with ':') are ignored. A blank line ends an event.
 */
public final class SseFrames {
    private final StringBuilder pending = new StringBuilder();
    private final StringBuilder data = new StringBuilder();
    private boolean inData;

    public List<String> push(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return List.of();
        }
        pending.append(chunk);
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < pending.length(); i++) {
            if (pending.charAt(i) != '\n') {
                continue;
            }
            int end = i;
            if (end > start && pending.charAt(end - 1) == '\r') {
                end--;
            }
            out.addAll(acceptLine(pending.substring(start, end)));
            start = i + 1;
        }
        if (start > 0) {
            pending.delete(0, start);
        }
        return out;
    }

    public List<String> acceptLine(String line) {
        if (line == null) {
            return List.of();
        }
        if (line.isEmpty()) {
            return flushEvent();
        }
        if (line.charAt(0) == ':') {
            return List.of();
        }
        if (line.startsWith("data:")) {
            String value = line.substring(5);
            if (value.startsWith(" ")) {
                value = value.substring(1);
            }
            if (inData && data.length() > 0) {
                data.append('\n');
            }
            data.append(value);
            inData = true;
        }
        return List.of();
    }

    public List<String> finish() {
        List<String> out = new ArrayList<>();
        if (pending.length() > 0) {
            String tail = pending.toString();
            pending.setLength(0);
            if (tail.endsWith("\r")) {
                tail = tail.substring(0, tail.length() - 1);
            }
            out.addAll(acceptLine(tail));
        }
        out.addAll(flushEvent());
        return out;
    }

    private List<String> flushEvent() {
        if (!inData) {
            return List.of();
        }
        String payload = data.toString();
        data.setLength(0);
        inData = false;
        return List.of(payload);
    }
}
