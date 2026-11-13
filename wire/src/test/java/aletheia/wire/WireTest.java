package aletheia.wire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class WireTest {
    @Test
    public void requestBodyEscapesTextAndOmitsABlankModel() throws Exception {
        String body = CompletionRequest.body(
                "  ",
                List.of(new ChatTurn("user", "say \"hi\"\nplease")),
                true);
        JSONObject json = new JSONObject(body);
        assertFalse(json.has("model"));
        assertTrue(json.getBoolean("stream"));
        assertEquals("say \"hi\"\nplease", json.getJSONArray("messages").getJSONObject(0).getString("content"));

        JSONObject named = new JSONObject(CompletionRequest.body(
                " openrouter/auto ",
                List.of(new ChatTurn("system", "Be plain.")),
                true));
        assertEquals("openrouter/auto", named.getString("model"));
        assertEquals("system", named.getJSONArray("messages").getJSONObject(0).getString("role"));
    }

    @Test
    public void framesSurviveSplitChunksCommentsAndDone() {
        SseFrames frames = new SseFrames();
        assertTrue(frames.push(": OPENROUTER PROCESSING\n\n").isEmpty());

        assertTrue(frames.push("data: {\"choices\":[{\"del").isEmpty());
        List<String> first = frames.push("ta\":{\"content\":\"Hel\"}}]}\n\n");
        assertEquals(1, first.size());
        assertEquals("Hel", CompletionParser.parse(first.get(0)).text);

        List<String> second = frames.push("data: {\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}\r\n\r\n");
        assertEquals("lo", CompletionParser.parse(second.get(0)).text);

        List<String> done = frames.push("data: [DONE]\n");
        assertTrue(done.isEmpty());
        List<String> finished = frames.finish();
        assertEquals(1, finished.size());
        assertTrue(CompletionParser.parse(finished.get(0)).done);
    }

    @Test
    public void parserReadsDeltasMessagesContentArraysAndErrors() {
        assertEquals("", CompletionParser.parse("{\"choices\":[{\"delta\":{\"content\":null}}]}").text);
        assertNull(CompletionParser.parse("{\"choices\":[{\"delta\":{\"content\":null}}]}").error);

        String parts = "{\"choices\":[{\"message\":{\"content\":[{\"type\":\"text\",\"text\":\"A\"},{\"type\":\"text\",\"text\":\"B\"}]}}]}";
        assertEquals("AB", CompletionParser.parse(parts).text);

        Piece failed = CompletionParser.parse("{\"error\":{\"message\":\"no credit\",\"code\":402}}");
        assertEquals("no credit", failed.error);

        Piece choiceError = CompletionParser.parse("{\"choices\":[{\"error\":{\"message\":\"filtered\",\"code\":400},\"delta\":{}}]}");
        assertEquals("filtered", choiceError.error);

        Piece malformed = CompletionParser.parse("not-json");
        assertEquals("Could not read the model response.", malformed.error);

        assertEquals("Missing Authentication header", CompletionParser.errorMessage(
                401,
                "{\"error\":{\"message\":\"Missing Authentication header\",\"code\":401}}"));
        assertEquals("OpenRouter returned 500", CompletionParser.errorMessage(500, "nope"));
    }

    @Test
    public void catalogSortsIdsAndSkipsBlanks() {
        String json = "{\"data\":[{\"id\":\"zeta/one\",\"name\":\"Zeta\"},{\"id\":\"\",\"name\":\"skip\"},{\"id\":\"alpha/two\"}]}";
        List<ModelEntry> entries = ModelCatalog.parse(json);
        assertEquals(2, entries.size());
        assertEquals("alpha/two", entries.get(0).id);
        assertEquals("alpha/two", entries.get(0).name);
        assertEquals("Zeta", entries.get(1).name);
        assertTrue(ModelCatalog.parse("not json").isEmpty());
    }

    @Test
    public void clientStreamsChunkedEventsAndSendsTheKey() throws Exception {
        MockWebServer server = new MockWebServer();
        String sse = ""
                + ": OPENROUTER PROCESSING\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"Hel\"}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}\n\n"
                + "data: [DONE]\n\n";
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setChunkedBody(sse, 9));
        server.start();
        try {
            OpenRouterClient client = new OpenRouterClient(server.url("/api/v1").toString());
            List<String> deltas = new ArrayList<>();
            client.stream("test-key", "openrouter/auto", List.of(new ChatTurn("user", "Hi")), deltas::add);
            assertEquals(List.of("Hel", "lo"), deltas);

            RecordedRequest request = server.takeRequest();
            assertEquals("/api/v1/chat/completions", request.getPath());
            assertEquals("Bearer test-key", request.getHeader("Authorization"));
            assertEquals("Aletheia", request.getHeader("X-OpenRouter-Title"));
            assertEquals("https://aletheia.app", request.getHeader("HTTP-Referer"));
            JSONObject body = new JSONObject(request.getBody().readUtf8());
            assertEquals("openrouter/auto", body.getString("model"));
            assertTrue(body.getBoolean("stream"));
            assertEquals("Hi", body.getJSONArray("messages").getJSONObject(0).getString("content"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void clientKeepsPartialTextWhenTheStreamFails() throws Exception {
        MockWebServer server = new MockWebServer();
        String sse = ""
                + "data: {\"choices\":[{\"delta\":{\"content\":\"Part\"}}]}\n\n"
                + "data: {\"error\":{\"message\":\"upstream failed\",\"code\":502}}\n\n";
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sse));
        server.start();
        try {
            OpenRouterClient client = new OpenRouterClient(server.url("/api/v1").toString());
            List<String> deltas = new ArrayList<>();
            try {
                client.stream("k", "m", List.of(new ChatTurn("user", "Hi")), deltas::add);
                fail("expected an upstream error");
            } catch (OpenRouterException exception) {
                assertEquals("upstream failed", exception.getMessage());
            }
            assertEquals(List.of("Part"), deltas);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void clientReadsHttpErrorsAndModelLists() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":{\"message\":\"Missing Authentication header\",\"code\":401}}"));
        server.enqueue(new MockResponse()
                .setBody("{\"data\":[{\"id\":\"b/model\",\"name\":\"B\"},{\"id\":\"a/model\",\"name\":\"A\"}]}"));
        server.start();
        try {
            OpenRouterClient client = new OpenRouterClient(server.url("/api/v1").toString());
            try {
                client.stream("bad", "m", List.of(new ChatTurn("user", "Hi")), text -> { });
                fail("expected a 401");
            } catch (OpenRouterException exception) {
                assertEquals("Missing Authentication header", exception.getMessage());
                assertEquals(401, exception.status);
            }
            List<ModelEntry> models = client.models("good-key");
            assertEquals("a/model", models.get(0).id);
            assertEquals("A", models.get(0).name);
            server.takeRequest();
            RecordedRequest modelsRequest = server.takeRequest();
            assertEquals("/api/v1/models", modelsRequest.getPath());
            assertEquals("Bearer good-key", modelsRequest.getHeader("Authorization"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void plainJsonCompletionIsAcceptedWhenTheServerDoesNotStream() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Whole\"}}]}"));
        server.start();
        try {
            OpenRouterClient client = new OpenRouterClient(server.url("/api/v1").toString());
            AtomicReference<String> text = new AtomicReference<>();
            client.stream("k", "m", List.of(new ChatTurn("user", "Hi")), text::set);
            assertEquals("Whole", text.get());
        } finally {
            server.shutdown();
        }
    }
}
