package uk.co.sexeys.ui.swing;

import uk.co.sexeys.rendering.Renderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Swing/AWT implementation of the Renderer interface.
 * Wraps a Graphics2D object to provide rendering capabilities.
 */
public class SwingRenderer implements Renderer {

    private Graphics2D g2d;
    private int width;
    private int height;
    private final ImageObserver imageObserver;
    private final Map<String, BufferedImage> imageCache;
    private final Stack<GraphicsState> stateStack;

    /**
     * Internal class to save graphics state.
     */
    private static class GraphicsState {
        Color color;
        Stroke stroke;
        Font font;
        AffineTransform transform;
        Shape clip;

        GraphicsState(Graphics2D g) {
            this.color = g.getColor();
            this.stroke = g.getStroke();
            this.font = g.getFont();
            this.transform = g.getTransform();
            this.clip = g.getClip();
        }

        void restore(Graphics2D g) {
            g.setColor(color);
            g.setStroke(stroke);
            g.setFont(font);
            g.setTransform(transform);
            g.setClip(clip);
        }
    }

    public SwingRenderer(ImageObserver observer) {
        this.imageObserver = observer;
        this.imageCache = new HashMap<>();
        this.stateStack = new Stack<>();
    }

    /**
     * Set the Graphics2D context for rendering.
     * Called before each frame.
     */
    public void setGraphics(Graphics2D g2d, int width, int height) {
        this.g2d = g2d;
        this.width = width;
        this.height = height;
    }

    /**
     * Get the underlying Graphics2D for direct access when needed.
     */
    public Graphics2D getGraphics2D() {
        return g2d;
    }

    /**
     * Register an image in the cache for later use.
     */
    public void registerImage(String key, BufferedImage image) {
        imageCache.put(key, image);
    }

    /**
     * Remove an image from the cache.
     */
    public void unregisterImage(String key) {
        imageCache.remove(key);
    }

    @Override
    public void setColor(int r, int g, int b) {
        g2d.setColor(new Color(r, g, b));
    }

    @Override
    public void setColor(int r, int g, int b, int alpha) {
        g2d.setColor(new Color(r, g, b, alpha));
    }

    @Override
    public void setStrokeWidth(float width) {
        g2d.setStroke(new BasicStroke(width));
    }

    @Override
    public void setDashedStroke(float width, float[] dashPattern) {
        g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashPattern, 0));
    }

    @Override
    public void setSolidStroke(float width) {
        g2d.setStroke(new BasicStroke(width));
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
    }

    @Override
    public void drawRect(float x, float y, float width, float height) {
        g2d.drawRect((int) x, (int) y, (int) width, (int) height);
    }

    @Override
    public void fillRect(float x, float y, float width, float height) {
        g2d.fillRect((int) x, (int) y, (int) width, (int) height);
    }

    @Override
    public void drawOval(float x, float y, float width, float height) {
        g2d.drawOval((int) x, (int) y, (int) width, (int) height);
    }

    @Override
    public void fillOval(float x, float y, float width, float height) {
        g2d.fillOval((int) x, (int) y, (int) width, (int) height);
    }

    @Override
    public void drawPolygon(float[] xPoints, float[] yPoints, int nPoints) {
        int[] xInts = new int[nPoints];
        int[] yInts = new int[nPoints];
        for (int i = 0; i < nPoints; i++) {
            xInts[i] = (int) xPoints[i];
            yInts[i] = (int) yPoints[i];
        }
        g2d.drawPolygon(xInts, yInts, nPoints);
    }

    @Override
    public void fillPolygon(float[] xPoints, float[] yPoints, int nPoints) {
        int[] xInts = new int[nPoints];
        int[] yInts = new int[nPoints];
        for (int i = 0; i < nPoints; i++) {
            xInts[i] = (int) xPoints[i];
            yInts[i] = (int) yPoints[i];
        }
        g2d.fillPolygon(xInts, yInts, nPoints);
    }

    @Override
    public void drawArc(float x, float y, float width, float height, int startAngle, int arcAngle) {
        g2d.drawArc((int) x, (int) y, (int) width, (int) height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(float x, float y, float width, float height, int startAngle, int arcAngle) {
        g2d.fillArc((int) x, (int) y, (int) width, (int) height, startAngle, arcAngle);
    }

    @Override
    public void drawText(String text, float x, float y) {
        g2d.drawString(text, x, y);
    }

    @Override
    public void setFont(String fontName, int style, int size) {
        g2d.setFont(new Font(fontName, style, size));
    }

    @Override
    public int getStringWidth(String text) {
        FontMetrics fm = g2d.getFontMetrics();
        return fm.stringWidth(text);
    }

    @Override
    public int getFontHeight() {
        FontMetrics fm = g2d.getFontMetrics();
        return fm.getHeight();
    }

    @Override
    public void drawImage(String imageKey, float destX1, float destY1, float destX2, float destY2,
                          int srcX1, int srcY1, int srcX2, int srcY2) {
        BufferedImage image = imageCache.get(imageKey);
        if (image != null) {
            g2d.drawImage(image, (int) destX1, (int) destY1, (int) destX2, (int) destY2,
                    srcX1, srcY1, srcX2, srcY2, imageObserver);
        }
    }

    /**
     * Draw an image directly (for cases where the image isn't cached).
     */
    public void drawImage(BufferedImage image, float destX1, float destY1, float destX2, float destY2,
                          int srcX1, int srcY1, int srcX2, int srcY2) {
        g2d.drawImage(image, (int) destX1, (int) destY1, (int) destX2, (int) destY2,
                srcX1, srcY1, srcX2, srcY2, imageObserver);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void save() {
        stateStack.push(new GraphicsState(g2d));
    }

    @Override
    public void restore() {
        if (!stateStack.isEmpty()) {
            stateStack.pop().restore(g2d);
        }
    }

    @Override
    public void setClip(float x, float y, float width, float height) {
        g2d.setClip((int) x, (int) y, (int) width, (int) height);
    }

    @Override
    public void clearClip() {
        g2d.setClip(null);
    }

    @Override
    public void translate(float dx, float dy) {
        g2d.translate(dx, dy);
    }

    @Override
    public void rotate(double angle) {
        g2d.rotate(angle);
    }

    @Override
    public void scale(float sx, float sy) {
        g2d.scale(sx, sy);
    }

    // Additional Swing-specific convenience methods

    /**
     * Set color using AWT Color directly (for backward compatibility).
     */
    public void setColor(Color color) {
        g2d.setColor(color);
    }

    /**
     * Get current AWT color.
     */
    public Color getColor() {
        return g2d.getColor();
    }

    /**
     * Set stroke using AWT Stroke directly (for backward compatibility).
     */
    public void setStroke(Stroke stroke) {
        g2d.setStroke(stroke);
    }

    /**
     * Get current AWT stroke.
     */
    public Stroke getStroke() {
        return g2d.getStroke();
    }

    /**
     * Set font using AWT Font directly (for backward compatibility).
     */
    public void setFont(Font font) {
        g2d.setFont(font);
    }

    /**
     * Get current AWT font.
     */
    public Font getFont() {
        return g2d.getFont();
    }
}
