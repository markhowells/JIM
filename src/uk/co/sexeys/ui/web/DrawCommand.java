package uk.co.sexeys.ui.web;

import java.util.List;
import java.util.Map;

/**
 * Represents a single drawing command that can be serialized to JSON
 * and executed by a browser Canvas context.
 */
public class DrawCommand {

    public enum Type {
        SET_COLOR,
        SET_STROKE_WIDTH,
        SET_FONT,
        DRAW_LINE,
        DRAW_RECT,
        FILL_RECT,
        DRAW_OVAL,
        FILL_OVAL,
        DRAW_ARC,
        FILL_ARC,
        DRAW_POLYGON,
        FILL_POLYGON,
        DRAW_POLYLINE,
        DRAW_STRING,
        DRAW_IMAGE,
        TRANSLATE,
        ROTATE,
        SCALE,
        SAVE,
        RESTORE,
        SET_CLIP,
        CLEAR_CLIP,
        CLEAR
    }

    private Type type;
    private Map<String, Object> params;
    private String layer;  // Layer tag for client-side caching: "static", "dynamic", "route", "ui"

    public DrawCommand() {}

    public DrawCommand(Type type, Map<String, Object> params) {
        this.type = type;
        this.params = params;
    }

    public DrawCommand(Type type, Map<String, Object> params, String layer) {
        this.type = type;
        this.params = params;
        this.layer = layer;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    // Factory methods for common commands

    public static DrawCommand setColor(int r, int g, int b) {
        return new DrawCommand(Type.SET_COLOR, Map.of("r", r, "g", g, "b", b));
    }

    public static DrawCommand setColor(int r, int g, int b, int a) {
        return new DrawCommand(Type.SET_COLOR, Map.of("r", r, "g", g, "b", b, "a", a));
    }

    public static DrawCommand setStrokeWidth(float width) {
        return new DrawCommand(Type.SET_STROKE_WIDTH, Map.of("width", width));
    }

    public static DrawCommand setFont(String name, int style, int size) {
        return new DrawCommand(Type.SET_FONT, Map.of("name", name, "style", style, "size", size));
    }

    public static DrawCommand drawLine(double x1, double y1, double x2, double y2) {
        return new DrawCommand(Type.DRAW_LINE, Map.of("x1", x1, "y1", y1, "x2", x2, "y2", y2));
    }

    public static DrawCommand drawRect(double x, double y, double width, double height) {
        return new DrawCommand(Type.DRAW_RECT, Map.of("x", x, "y", y, "width", width, "height", height));
    }

    public static DrawCommand fillRect(double x, double y, double width, double height) {
        return new DrawCommand(Type.FILL_RECT, Map.of("x", x, "y", y, "width", width, "height", height));
    }

    public static DrawCommand drawOval(double x, double y, double width, double height) {
        return new DrawCommand(Type.DRAW_OVAL, Map.of("x", x, "y", y, "width", width, "height", height));
    }

    public static DrawCommand fillOval(double x, double y, double width, double height) {
        return new DrawCommand(Type.FILL_OVAL, Map.of("x", x, "y", y, "width", width, "height", height));
    }

    public static DrawCommand drawArc(double x, double y, double width, double height,
                                       double startAngle, double arcAngle) {
        return new DrawCommand(Type.DRAW_ARC, Map.of(
            "x", x, "y", y, "width", width, "height", height,
            "startAngle", startAngle, "arcAngle", arcAngle
        ));
    }

    public static DrawCommand fillArc(double x, double y, double width, double height,
                                       double startAngle, double arcAngle) {
        return new DrawCommand(Type.FILL_ARC, Map.of(
            "x", x, "y", y, "width", width, "height", height,
            "startAngle", startAngle, "arcAngle", arcAngle
        ));
    }

    public static DrawCommand drawPolygon(List<Double> xPoints, List<Double> yPoints) {
        return new DrawCommand(Type.DRAW_POLYGON, Map.of("xPoints", xPoints, "yPoints", yPoints));
    }

    public static DrawCommand fillPolygon(List<Double> xPoints, List<Double> yPoints) {
        return new DrawCommand(Type.FILL_POLYGON, Map.of("xPoints", xPoints, "yPoints", yPoints));
    }

    public static DrawCommand drawPolyline(List<Double> xPoints, List<Double> yPoints) {
        return new DrawCommand(Type.DRAW_POLYLINE, Map.of("xPoints", xPoints, "yPoints", yPoints));
    }

    public static DrawCommand drawString(String text, double x, double y) {
        return new DrawCommand(Type.DRAW_STRING, Map.of("text", text, "x", x, "y", y));
    }

    public static DrawCommand drawImage(String imageKey, double x, double y, double width, double height) {
        return new DrawCommand(Type.DRAW_IMAGE, Map.of(
            "imageKey", imageKey, "x", x, "y", y, "width", width, "height", height
        ));
    }

    public static DrawCommand translate(double tx, double ty) {
        return new DrawCommand(Type.TRANSLATE, Map.of("tx", tx, "ty", ty));
    }

    public static DrawCommand rotate(double theta) {
        return new DrawCommand(Type.ROTATE, Map.of("theta", theta));
    }

    public static DrawCommand rotate(double theta, double x, double y) {
        return new DrawCommand(Type.ROTATE, Map.of("theta", theta, "x", x, "y", y));
    }

    public static DrawCommand scale(double sx, double sy) {
        return new DrawCommand(Type.SCALE, Map.of("sx", sx, "sy", sy));
    }

    public static DrawCommand save() {
        return new DrawCommand(Type.SAVE, Map.of());
    }

    public static DrawCommand restore() {
        return new DrawCommand(Type.RESTORE, Map.of());
    }

    public static DrawCommand setClip(double x, double y, double width, double height) {
        return new DrawCommand(Type.SET_CLIP, Map.of("x", x, "y", y, "width", width, "height", height));
    }

    public static DrawCommand clearClip() {
        return new DrawCommand(Type.CLEAR_CLIP, Map.of());
    }

    public static DrawCommand clear(double width, double height) {
        return new DrawCommand(Type.CLEAR, Map.of("width", width, "height", height));
    }
}
