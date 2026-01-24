package uk.co.sexeys.input;

/**
 * Base class for all platform-agnostic input events.
 */
public abstract class InputEvent {
    private final long timestamp;
    private boolean consumed;

    protected InputEvent() {
        this.timestamp = System.currentTimeMillis();
        this.consumed = false;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void consume() {
        this.consumed = true;
    }
}
