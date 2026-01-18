package uk.co.sexeys.rendering;

/**
 * Abstract rendering interface that decouples visualization from domain logic.
 * Implementations can target Swing/AWT, web canvas, or other rendering backends.
 */
public interface Renderer {

    // Font style constants (matching java.awt.Font)
    int FONT_PLAIN = 0;
    int FONT_BOLD = 1;
    int FONT_ITALIC = 2;

    /**
     * Set the current drawing color using RGB values (0-255).
     */
    void setColor(int r, int g, int b);

    /**
     * Set the current drawing color using RGBA values (0-255).
     */
    void setColor(int r, int g, int b, int alpha);

    /**
     * Set the current drawing color using an RGB array.
     */
    default void setColor(int[] rgb) {
        if (rgb.length >= 4) {
            setColor(rgb[0], rgb[1], rgb[2], rgb[3]);
        } else {
            setColor(rgb[0], rgb[1], rgb[2]);
        }
    }

    /**
     * Set line stroke width.
     */
    void setStrokeWidth(float width);

    /**
     * Set dashed line style.
     */
    void setDashedStroke(float width, float[] dashPattern);

    /**
     * Set solid line style.
     */
    void setSolidStroke(float width);

    /**
     * Draw a line between two screen coordinates.
     */
    void drawLine(float x1, float y1, float x2, float y2);

    /**
     * Draw a polyline connecting multiple points.
     */
    default void drawPolyline(float[] xPoints, float[] yPoints, int nPoints) {
        for (int i = 0; i < nPoints - 1; i++) {
            drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
        }
    }

    /**
     * Draw a rectangle outline.
     */
    void drawRect(float x, float y, float width, float height);

    /**
     * Fill a rectangle.
     */
    void fillRect(float x, float y, float width, float height);

    /**
     * Draw an oval outline.
     */
    void drawOval(float x, float y, float width, float height);

    /**
     * Fill an oval.
     */
    void fillOval(float x, float y, float width, float height);

    /**
     * Draw a polygon outline.
     */
    void drawPolygon(float[] xPoints, float[] yPoints, int nPoints);

    /**
     * Fill a polygon.
     */
    void fillPolygon(float[] xPoints, float[] yPoints, int nPoints);

    /**
     * Draw an arc outline.
     */
    void drawArc(float x, float y, float width, float height, int startAngle, int arcAngle);

    /**
     * Fill an arc (pie slice).
     */
    void fillArc(float x, float y, float width, float height, int startAngle, int arcAngle);

    /**
     * Draw text at the specified position.
     */
    void drawText(String text, float x, float y);

    /**
     * Set font for text rendering.
     */
    void setFont(String fontName, int style, int size);

    /**
     * Get the width of a string in the current font.
     */
    int getStringWidth(String text);

    /**
     * Get the height of the current font.
     */
    int getFontHeight();

    /**
     * Draw an image at the specified screen coordinates.
     * The image is identified by a key (path or identifier).
     */
    void drawImage(String imageKey, float destX1, float destY1, float destX2, float destY2,
                   int srcX1, int srcY1, int srcX2, int srcY2);

    /**
     * Get the current screen/viewport width.
     */
    int getWidth();

    /**
     * Get the current screen/viewport height.
     */
    int getHeight();

    /**
     * Save the current graphics state (color, stroke, transform, etc).
     */
    void save();

    /**
     * Restore the previously saved graphics state.
     */
    void restore();

    /**
     * Set a clipping region.
     */
    void setClip(float x, float y, float width, float height);

    /**
     * Clear the clipping region.
     */
    void clearClip();

    /**
     * Translate the coordinate system.
     */
    void translate(float dx, float dy);

    /**
     * Rotate the coordinate system around the origin.
     * @param angle Angle in radians
     */
    void rotate(double angle);

    /**
     * Scale the coordinate system.
     */
    void scale(float sx, float sy);
}
