package aletheia.wire;

@FunctionalInterface
public interface DeltaSink {
    void onDelta(String text);
}
