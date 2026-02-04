package uk.co.sexeys.ui.web;

import uk.co.sexeys.rendering.Renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Renderer implementation that captures drawing commands as serializable objects.
 * These commands can be sent to a browser client for execution on HTML5 Canvas.
 */
public class WebRenderer implements Renderer {

    private final List<DrawCommand> commands = new ArrayList<>();
    private int width;
    private int height;

    // Current layer for client-side caching
    private String currentLayer = "ui";

    // Current state for queries
    private String currentFont = "Arial";
    private int currentFontStyle = FONT_PLAIN;
    private int currentFontSize = 12;

    // State stack for save/restore
    private final Stack<RendererState> stateStack = new Stack<>();

    private static class RendererState {
        String font;
        int fontStyle;
        int fontSize;

        RendererState(String font, int fontStyle, int fontSize) {
            this.font = font;
            this.fontStyle = fontStyle;
            this.fontSize = fontSize;
        }
    }

    public WebRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Get the list of accumulated drawing commands.
     */
    public List<DrawCommand> getCommands() {
        return commands;
    }

    /**
     * Set the current layer for subsequent commands.
     * Layer tags are used by the client for caching: "static", "dynamic", "route", "ui"
     */
    public void setCurrentLayer(String layer) {
        this.currentLayer = layer;
    }

    /**
     * Add a command and tag it with the current layer.
     */
    private void addCommand(DrawCommand cmd) {
        cmd.setLayer(currentLayer);
        commands.add(cmd);
    }

    /**
     * Clear accumulated commands and prepare for a new frame.
     */
    public void clear() {
        commands.clear();
        DrawCommand cmd = DrawCommand.clear(width, height);
        cmd.setLayer("ui");
        commands.add(cmd);
    }

    /**
     * Update the canvas dimensions.
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void setColor(int r, int g, int b) {
        addCommand(DrawCommand.setColor(r, g, b));
    }

    @Override
    public void setColor(int r, int g, int b, int alpha) {
        addCommand(DrawCommand.setColor(r, g, b, alpha));
    }

    @Override
    public void setStrokeWidth(float width) {
        addCommand(DrawCommand.setStrokeWidth(width));
    }

    @Override
    public void setDashedStroke(float width, float[] dashPattern) {
        List<Double> pattern = new ArrayList<>();
        for (float f : dashPattern) {
            pattern.add((double) f);
        }
        addCommand(new DrawCommand(DrawCommand.Type.SET_STROKE_WIDTH,
            Map.of("width", (double) width, "dashPattern", pattern)));
    }

    @Override
    public void setSolidStroke(float width) {
        addCommand(new DrawCommand(DrawCommand.Type.SET_STROKE_WIDTH,
            Map.of("width", (double) width, "dashPattern", List.of())));
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        addCommand(DrawCommand.drawLine(x1, y1, x2, y2));
    }

    @Override
    public void drawRect(float x, float y, float width, float height) {
        addCommand(DrawCommand.drawRect(x, y, width, height));
    }

    @Override
    public void fillRect(float x, float y, float width, float height) {
        addCommand(DrawCommand.fillRect(x, y, width, height));
    }

    @Override
    public void drawOval(float x, float y, float width, float height) {
        addCommand(DrawCommand.drawOval(x, y, width, height));
    }

    @Override
    public void fillOval(float x, float y, float width, float height) {
        addCommand(DrawCommand.fillOval(x, y, width, height));
    }

    @Override
    public void drawPolygon(float[] xPoints, float[] yPoints, int nPoints) {
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (int i = 0; i < nPoints; i++) {
            xs.add((double) xPoints[i]);
            ys.add((double) yPoints[i]);
        }
        addCommand(DrawCommand.drawPolygon(xs, ys));
    }

    @Override
    public void fillPolygon(float[] xPoints, float[] yPoints, int nPoints) {
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (int i = 0; i < nPoints; i++) {
            xs.add((double) xPoints[i]);
            ys.add((double) yPoints[i]);
        }
        addCommand(DrawCommand.fillPolygon(xs, ys));
    }

    @Override
    public void drawPolyline(float[] xPoints, float[] yPoints, int nPoints) {
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (int i = 0; i < nPoints; i++) {
            xs.add((double) xPoints[i]);
            ys.add((double) yPoints[i]);
        }
        addCommand(DrawCommand.drawPolyline(xs, ys));
    }

    @Override
    public void drawArc(float x, float y, float width, float height, int startAngle, int arcAngle) {
        addCommand(DrawCommand.drawArc(x, y, width, height, startAngle, arcAngle));
    }

    @Override
    public void fillArc(float x, float y, float width, float height, int startAngle, int arcAngle) {
        addCommand(DrawCommand.fillArc(x, y, width, height, startAngle, arcAngle));
    }

    @Override
    public void drawText(String text, float x, float y) {
        addCommand(DrawCommand.drawString(text, x, y));
    }

    @Override
    public void setFont(String fontName, int style, int size) {
        this.currentFont = fontName;
        this.currentFontStyle = style;
        this.currentFontSize = size;
        addCommand(DrawCommand.setFont(fontName, style, size));
    }

    @Override
    public int getStringWidth(String text) {
        // Approximate string width based on font size
        // In a real implementation, this would need font metrics from the client
        return text.length() * (currentFontSize * 6 / 10);
    }

    @Override
    public int getFontHeight() {
        return currentFontSize;
    }

    @Override
    public void drawImage(String imageKey, float destX1, float destY1, float destX2, float destY2,
                          int srcX1, int srcY1, int srcX2, int srcY2) {
        addCommand(new DrawCommand(DrawCommand.Type.DRAW_IMAGE, Map.of(
            "imageKey", imageKey,
            "destX1", (double) destX1,
            "destY1", (double) destY1,
            "destX2", (double) destX2,
            "destY2", (double) destY2,
            "srcX1", srcX1,
            "srcY1", srcY1,
            "srcX2", srcX2,
            "srcY2", srcY2
        )));
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
        stateStack.push(new RendererState(currentFont, currentFontStyle, currentFontSize));
        addCommand(DrawCommand.save());
    }

    @Override
    public void restore() {
        if (!stateStack.isEmpty()) {
            RendererState state = stateStack.pop();
            currentFont = state.font;
            currentFontStyle = state.fontStyle;
            currentFontSize = state.fontSize;
        }
        addCommand(DrawCommand.restore());
    }

    @Override
    public void setClip(float x, float y, float width, float height) {
        addCommand(DrawCommand.setClip(x, y, width, height));
    }

    @Override
    public void clearClip() {
        addCommand(DrawCommand.clearClip());
    }

    @Override
    public void translate(float dx, float dy) {
        addCommand(DrawCommand.translate(dx, dy));
    }

    @Override
    public void rotate(double angle) {
        addCommand(DrawCommand.rotate(angle));
    }

    @Override
    public void scale(float sx, float sy) {
        addCommand(DrawCommand.scale(sx, sy));
    }
}
