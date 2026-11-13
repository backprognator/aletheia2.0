package aletheia.wire;

public final class ModelEntry {
    public final String id;
    public final String name;

    public ModelEntry(String id, String name) {
        this.id = id;
        this.name = name == null || name.isBlank() ? id : name;
    }
}
