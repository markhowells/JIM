/**
 * Rendering abstraction layer for JIM.
 *
 * This package contains interfaces and classes that decouple the visualization
 * logic from specific rendering implementations (like Swing/AWT).
 *
 * <h2>Key Interfaces:</h2>
 * <ul>
 *   <li>{@link uk.co.sexeys.rendering.Renderer} - Abstract drawing operations</li>
 *   <li>{@link uk.co.sexeys.rendering.Projection} - Geographic to screen coordinate conversion</li>
 *   <li>{@link uk.co.sexeys.rendering.Renderable} - Interface for objects that can be drawn</li>
 * </ul>
 *
 * <h2>Usage Pattern:</h2>
 * <pre>
 * // Domain objects implement Renderable:
 * public class Route implements Renderable {
 *     public void render(Renderer renderer, Projection projection, long time) {
 *         Vector2 p = projection.fromRadiansToPoint(position);
 *         renderer.drawLine(p.x, p.y, ...);
 *     }
 * }
 *
 * // UI creates a Renderer implementation:
 * SwingRenderer renderer = new SwingRenderer(panel);
 * renderer.setGraphics(g2d, width, height);
 *
 * // Render domain objects:
 * route.render(renderer, projection, displayTime);
 * </pre>
 *
 * <h2>Benefits:</h2>
 * <ul>
 *   <li>Domain objects don't depend on java.awt</li>
 *   <li>Easy to add web/canvas rendering backend</li>
 *   <li>Testable without graphics context</li>
 * </ul>
 */
package uk.co.sexeys.rendering;
