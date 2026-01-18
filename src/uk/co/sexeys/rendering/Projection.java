package uk.co.sexeys.rendering;

import uk.co.sexeys.Vector2;

/**
 * Interface for map projections that convert between geographic coordinates
 * and screen coordinates. This abstracts the Mercator projection to allow
 * different projection implementations.
 */
public interface Projection {

    /**
     * Convert latitude/longitude (in degrees) to screen coordinates.
     *
     * @param lat Latitude in degrees
     * @param lng Longitude in degrees
     * @return Screen coordinates as Vector2
     */
    Vector2 fromLatLngToPoint(double lat, double lng);

    /**
     * Convert position in radians to screen coordinates.
     *
     * @param position Position with x=longitude, y=latitude in radians
     * @return Screen coordinates as Vector2
     */
    Vector2 fromRadiansToPoint(Vector2 position);

    /**
     * Convert screen coordinates to latitude/longitude (in degrees).
     *
     * @param x Screen x coordinate
     * @param y Screen y coordinate
     * @return Geographic coordinates as Vector2 (x=longitude, y=latitude in degrees)
     */
    Vector2 fromPointToLatLng(double x, double y);

    /**
     * Convert a length in meters to pixels at the given position.
     *
     * @param position Geographic position in radians
     * @param lengthMeters Length in meters
     * @return Length in pixels
     */
    double fromLengthToPixels(Vector2 position, float lengthMeters);

    /**
     * Get the current scale (meters per degree latitude, approximately).
     */
    float getScale();

    /**
     * Get the viewport width.
     */
    int getWidth();

    /**
     * Get the viewport height.
     */
    int getHeight();

    /**
     * Get the top-left corner in geographic coordinates.
     */
    Vector2 getTopLeft();

    /**
     * Get the bottom-right corner in geographic coordinates.
     */
    Vector2 getBottomRight();
}
