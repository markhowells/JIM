package uk.co.sexeys.input;

/**
 * Platform-agnostic component event (resize, etc.).
 */
public class ComponentEvent extends InputEvent {

    public enum Type {
        RESIZED,
        SHOWN,
        HIDDEN
    }

    private final Type type;
    private final int width;
    private final int height;

    public ComponentEvent(Type type, int width, int height) {
        this.type = type;
        this.width = width;
        this.height = height;
    }

    public Type getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
