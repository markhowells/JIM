/**
 * Swing/AWT UI implementation for JIM.
 *
 * This package contains all Swing-specific UI code, isolated from the
 * business logic and domain model. This allows the core application
 * to be reused with different UI frameworks (e.g., web).
 *
 * <h2>Key Classes:</h2>
 * <ul>
 *   <li>{@link uk.co.sexeys.ui.swing.SwingRenderer} - Renderer implementation for Swing</li>
 * </ul>
 *
 * <h2>Migration Path:</h2>
 * The StreamFrame class should eventually be refactored to use the service layer
 * and rendering abstractions. The goal is:
 *
 * <pre>
 * StreamFrame (UI Controller)
 *     |
 *     +-- ApplicationContext (Services)
 *     |       |-- RoutingService
 *     |       |-- WeatherService
 *     |       +-- ChartService
 *     |
 *     +-- SwingRenderer (Rendering)
 *             |-- MercatorProjection
 *             +-- RenderContext
 * </pre>
 *
 * <h2>Event Handling:</h2>
 * The StreamPanel handles all mouse/keyboard events. These should be refactored
 * to call methods on ApplicationContext rather than directly manipulating
 * domain objects. For example:
 *
 * <pre>
 * // Current (tightly coupled):
 * boat.DE = new DifferentialEvolution(...);
 *
 * // Target (loosely coupled):
 * applicationContext.optimizeRoute(...);
 * </pre>
 */
package uk.co.sexeys.ui.swing;
