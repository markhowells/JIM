package uk.co.sexeys.rendering;

/**
 * Interface for objects that can be rendered to a display.
 * This decouples domain objects from specific rendering implementations.
 */
public interface Renderable {

    /**
     * Render this object using the provided renderer and projection.
     *
     * @param renderer The renderer to draw with
     * @param projection The coordinate projection for geo-to-screen conversion
     * @param time The current display time in milliseconds (for time-varying displays)
     */
    void render(Renderer renderer, Projection projection, long time);
}
