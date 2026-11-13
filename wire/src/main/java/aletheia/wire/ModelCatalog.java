package aletheia.wire;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ModelCatalog {
    private ModelCatalog() {}

    public static List<ModelEntry> parse(String json) {
        List<ModelEntry> entries = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return entries;
        }
        try {
            JSONArray data = new JSONObject(json).optJSONArray("data");
            if (data == null) {
                return entries;
            }
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null || !item.has("id") || item.isNull("id")) {
                    continue;
                }
                String id = item.getString("id").trim();
                if (id.isEmpty()) {
                    continue;
                }
                String name = item.has("name") && !item.isNull("name") ? item.getString("name") : id;
                entries.add(new ModelEntry(id, name));
            }
        } catch (JSONException ignored) {
            return entries;
        }
        entries.sort(Comparator.comparing(entry -> entry.id));
        return entries;
    }
}
