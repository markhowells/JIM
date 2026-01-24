/**
 * Platform-agnostic input handling abstraction.
 *
 * This package provides abstract input event types and interfaces that allow
 * the application's input handling logic to be decoupled from specific UI frameworks.
 *
 * Key components:
 * - {@link uk.co.sexeys.input.InputEvent} - Base class for all input events
 * - {@link uk.co.sexeys.input.KeyEvent} - Keyboard input events
 * - {@link uk.co.sexeys.input.MouseEvent} - Mouse input events
 * - {@link uk.co.sexeys.input.ComponentEvent} - Component events (resize, etc.)
 * - {@link uk.co.sexeys.input.InputHandler} - Interface for receiving events
 *
 * Platform-specific adapters (e.g., SwingInputAdapter) translate native events
 * to these abstract types.
 */
package uk.co.sexeys.input;
