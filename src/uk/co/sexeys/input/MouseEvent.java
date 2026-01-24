package uk.co.sexeys.input;

/**
 * Platform-agnostic mouse event.
 */
public class MouseEvent extends InputEvent {

    public enum Type {
        PRESSED,
        RELEASED,
        CLICKED,
        MOVED,
        DRAGGED,
        WHEEL
    }

    public enum Button {
        NONE,
        LEFT,
        MIDDLE,
        RIGHT
    }

    private final Type type;
    private final double x;
    private final double y;
    private final Button button;
    private final int wheelRotation;
    private final boolean shiftDown;
    private final boolean ctrlDown;
    private final boolean altDown;

    public MouseEvent(Type type, double x, double y, Button button, int wheelRotation,
                      boolean shiftDown, boolean ctrlDown, boolean altDown) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.button = button;
        this.wheelRotation = wheelRotation;
        this.shiftDown = shiftDown;
        this.ctrlDown = ctrlDown;
        this.altDown = altDown;
    }

    // Convenience constructor for non-wheel events
    public MouseEvent(Type type, double x, double y, Button button,
                      boolean shiftDown, boolean ctrlDown, boolean altDown) {
        this(type, x, y, button, 0, shiftDown, ctrlDown, altDown);
    }

    public Type getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Button getButton() {
        return button;
    }

    public int getWheelRotation() {
        return wheelRotation;
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    public boolean isCtrlDown() {
        return ctrlDown;
    }

    public boolean isAltDown() {
        return altDown;
    }

    public boolean isLeftButton() {
        return button == Button.LEFT;
    }

    public boolean isRightButton() {
        return button == Button.RIGHT;
    }

    public boolean isMiddleButton() {
        return button == Button.MIDDLE;
    }
}
