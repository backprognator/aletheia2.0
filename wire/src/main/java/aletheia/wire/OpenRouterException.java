package aletheia.wire;

import java.io.IOException;

public final class OpenRouterException extends IOException {
    public final int status;

    public OpenRouterException(String message, int status) {
        super(message);
        this.status = status;
    }
}
