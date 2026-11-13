package aletheia.wire;

/** One parsed server-sent event from a completion stream. */
public final class Piece {
    public final String text;
    public final String error;
    public final boolean done;

    private Piece(String text, String error, boolean done) {
        this.text = text;
        this.error = error;
        this.done = done;
    }

    public static Piece text(String text) {
        return new Piece(text == null ? "" : text, null, false);
    }

    public static Piece error(String error) {
        return new Piece("", error == null ? "OpenRouter error" : error, false);
    }

    public static Piece done() {
        return new Piece("", null, true);
    }
}
