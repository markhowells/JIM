package uk.co.sexeys.input;

/**
 * Platform-agnostic key event.
 */
public class KeyEvent extends InputEvent {

    public enum Type {
        PRESSED,
        RELEASED,
        TYPED
    }

    public enum Modifier {
        SHIFT,
        CTRL,
        ALT
    }

    // Common key codes (platform-agnostic)
    public static final int KEY_SPACE = 32;
    public static final int KEY_PLUS = 43;
    public static final int KEY_EQUALS = 61;
    public static final int KEY_MINUS = 45;
    public static final int KEY_UNDERSCORE = 95;
    public static final int KEY_0 = 48;
    public static final int KEY_1 = 49;
    public static final int KEY_2 = 50;
    public static final int KEY_3 = 51;
    public static final int KEY_4 = 52;
    public static final int KEY_5 = 53;
    public static final int KEY_6 = 54;
    public static final int KEY_7 = 55;
    public static final int KEY_8 = 56;
    public static final int KEY_9 = 57;
    public static final int KEY_A = 65;
    public static final int KEY_C = 67;
    public static final int KEY_D = 68;
    public static final int KEY_F = 70;
    public static final int KEY_G = 71;
    public static final int KEY_H = 72;
    public static final int KEY_I = 73;
    public static final int KEY_O = 79;
    public static final int KEY_P = 80;
    public static final int KEY_Q = 81;
    public static final int KEY_R = 82;
    public static final int KEY_S = 83;
    public static final int KEY_T = 84;
    public static final int KEY_U = 85;
    public static final int KEY_W = 87;
    public static final int KEY_X = 88;
    public static final int KEY_Z = 90;
    public static final int KEY_OPEN_BRACKET = 91;
    public static final int KEY_CLOSE_BRACKET = 93;
    public static final int KEY_QUESTION = 63;

    private final Type type;
    private final char keyChar;
    private final int keyCode;
    private final boolean shiftDown;
    private final boolean ctrlDown;
    private final boolean altDown;

    public KeyEvent(Type type, char keyChar, int keyCode, boolean shiftDown, boolean ctrlDown, boolean altDown) {
        this.type = type;
        this.keyChar = keyChar;
        this.keyCode = keyCode;
        this.shiftDown = shiftDown;
        this.ctrlDown = ctrlDown;
        this.altDown = altDown;
    }

    public Type getType() {
        return type;
    }

    public char getKeyChar() {
        return keyChar;
    }

    public int getKeyCode() {
        return keyCode;
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

    public boolean hasModifier(Modifier modifier) {
        return switch (modifier) {
            case SHIFT -> shiftDown;
            case CTRL -> ctrlDown;
            case ALT -> altDown;
        };
    }
}
