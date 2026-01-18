package uk.co.sexeys.rendering;

import uk.co.sexeys.Mercator;
import uk.co.sexeys.Vector2;

/**
 * Adapter that wraps the existing Mercator class to implement the Projection interface.
 * This allows gradual migration of rendering code to use the abstraction.
 */
public class MercatorProjection implements Projection {

    private final Mercator mercator;

    public MercatorProjection(Mercator mercator) {
        this.mercator = mercator;
    }

    /**
     * Get the underlying Mercator instance for backwards compatibility.
     */
    public Mercator getMercator() {
        return mercator;
    }

    @Override
    public Vector2 fromLatLngToPoint(double lat, double lng) {
        return mercator.fromLatLngToPoint(lat, lng);
    }

    @Override
    public Vector2 fromRadiansToPoint(Vector2 position) {
        return mercator.fromRadiansToPoint(position);
    }

    @Override
    public Vector2 fromPointToLatLng(double x, double y) {
        return mercator.fromPointToLatLng(x, y);
    }

    @Override
    public double fromLengthToPixels(Vector2 position, float lengthMeters) {
        return mercator.fromLengthToPixels(position, lengthMeters);
    }

    @Override
    public float getScale() {
        return mercator.scale;
    }

    @Override
    public int getWidth() {
        return mercator.width;
    }

    @Override
    public int getHeight() {
        return mercator.height;
    }

    @Override
    public Vector2 getTopLeft() {
        return mercator.topLeft;
    }

    @Override
    public Vector2 getBottomRight() {
        return mercator.bottomRight;
    }
}
