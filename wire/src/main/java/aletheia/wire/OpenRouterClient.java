package aletheia.wire;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

/**
 * Talks to OpenRouter's chat completions API.
 * The key is sent as a bearer token and is not logged.
 */
public final class OpenRouterClient {
    public static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String REFERRER = "https://aletheia.app";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http;
    private final String baseUrl;
    private volatile Call streamCall;

    public OpenRouterClient() {
        this(defaultHttp(), DEFAULT_BASE_URL);
    }

    public OpenRouterClient(String baseUrl) {
        this(defaultHttp(), baseUrl);
    }

    public OpenRouterClient(OkHttpClient http, String baseUrl) {
        this.http = http;
        String trimmed = baseUrl == null ? DEFAULT_BASE_URL : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.baseUrl = trimmed;
    }

    public void cancel() {
        Call call = streamCall;
        if (call != null) {
            call.cancel();
        }
    }

    public void stream(String apiKey, String model, List<ChatTurn> turns, DeltaSink sink) throws IOException {
        byte[] json = CompletionRequest.body(model, turns, true).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Request request = authorized(apiKey)
                .url(baseUrl + "/chat/completions")
                .header("Accept", "text/event-stream, application/json")
                .post(RequestBody.create(json, JSON))
                .build();
        Call call = http.newCall(request);
        streamCall = call;
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() == null ? "" : response.body().string();
                throw new OpenRouterException(CompletionParser.errorMessage(response.code(), err), response.code());
            }
            if (response.body() == null) {
                throw new OpenRouterException("OpenRouter returned an empty body.", response.code());
            }
            BufferedSource source = response.body().source();
            SseFrames frames = new SseFrames();
            boolean sawFrame = false;
            boolean done = false;
            StringBuilder raw = new StringBuilder();
            while (!done) {
                if (Thread.currentThread().isInterrupted()) {
                    call.cancel();
                    throw new InterruptedIOException("cancelled");
                }
                String line = source.readUtf8Line();
                if (line == null) {
                    break;
                }
                raw.append(line).append('\n');
                for (String payload : frames.acceptLine(line)) {
                    sawFrame = true;
                    done = emit(payload, sink);
                }
            }
            if (!done) {
                for (String payload : frames.finish()) {
                    sawFrame = true;
                    done = emit(payload, sink);
                    if (done) {
                        break;
                    }
                }
            }
            if (!sawFrame) {
                String leftover = raw.toString().trim();
                if (leftover.startsWith("{")) {
                    emit(leftover, sink);
                }
            }
        } finally {
            if (streamCall == call) {
                streamCall = null;
            }
        }
    }

    public List<ModelEntry> models(String apiKey) throws IOException {
        Request request = authorized(apiKey)
                .url(baseUrl + "/models")
                .header("Accept", "application/json")
                .get()
                .build();
        try (Response response = http.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new OpenRouterException(CompletionParser.errorMessage(response.code(), body), response.code());
            }
            return ModelCatalog.parse(body);
        }
    }

    private static Request.Builder authorized(String apiKey) {
        return new Request.Builder()
                .header("Authorization", "Bearer " + (apiKey == null ? "" : apiKey.trim()))
                .header("HTTP-Referer", REFERRER)
                .header("X-OpenRouter-Title", "Aletheia");
    }

    private static boolean emit(String payload, DeltaSink sink) throws IOException {
        Piece piece = CompletionParser.parse(payload);
        if (piece.error != null) {
            throw new OpenRouterException(piece.error, 0);
        }
        if (piece.done) {
            return true;
        }
        if (!piece.text.isEmpty() && sink != null) {
            sink.onDelta(piece.text);
        }
        return false;
    }

    private static OkHttpClient defaultHttp() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
}
