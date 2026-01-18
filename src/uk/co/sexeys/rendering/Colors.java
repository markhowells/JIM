package uk.co.sexeys.rendering;

/**
 * Color constants for rendering, independent of java.awt.Color.
 * Each color is represented as RGB values (0-255).
 */
public final class Colors {
    private Colors() {} // Prevent instantiation

    // Standard colors
    public static final int[] BLACK = {0, 0, 0};
    public static final int[] WHITE = {255, 255, 255};
    public static final int[] RED = {255, 0, 0};
    public static final int[] GREEN = {0, 255, 0};
    public static final int[] BLUE = {0, 0, 255};
    public static final int[] YELLOW = {255, 255, 0};
    public static final int[] CYAN = {0, 255, 255};
    public static final int[] MAGENTA = {255, 0, 255};
    public static final int[] ORANGE = {255, 200, 0};
    public static final int[] PINK = {255, 175, 175};
    public static final int[] GRAY = {128, 128, 128};
    public static final int[] LIGHT_GRAY = {192, 192, 192};
    public static final int[] DARK_GRAY = {64, 64, 64};

    // Application-specific colors
    public static final int[] ROUTE_LINE = {0, 0, 0};
    public static final int[] ROUTE_HIGHLIGHT = {255, 0, 0};
    public static final int[] WIND_ARROW = {0, 100, 200};
    public static final int[] TIDE_ARROW = {100, 0, 200};
    public static final int[] WAVE_WARNING = {255, 0, 0};
    public static final int[] DAYLIGHT_TRACK = {0, 0, 0};
    public static final int[] NIGHT_TRACK = {100, 100, 100};

    /**
     * Create a color from HSB values.
     * @param hue 0.0-1.0
     * @param saturation 0.0-1.0
     * @param brightness 0.0-1.0
     * @return RGB array
     */
    public static int[] fromHSB(float hue, float saturation, float brightness) {
        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
        return new int[]{
            (rgb >> 16) & 0xFF,
            (rgb >> 8) & 0xFF,
            rgb & 0xFF
        };
    }

    /**
     * Interpolate between two colors.
     * @param c1 First color
     * @param c2 Second color
     * @param t Interpolation factor (0.0 = c1, 1.0 = c2)
     * @return Interpolated color
     */
    public static int[] interpolate(int[] c1, int[] c2, float t) {
        return new int[]{
            (int)(c1[0] + (c2[0] - c1[0]) * t),
            (int)(c1[1] + (c2[1] - c1[1]) * t),
            (int)(c1[2] + (c2[2] - c1[2]) * t)
        };
    }
}
