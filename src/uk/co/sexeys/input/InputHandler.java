package uk.co.sexeys.input;

/**
 * Interface for handling platform-agnostic input events.
 * Implement this interface to receive input from any platform adapter.
 */
public interface InputHandler {

    /**
     * Called when a key is pressed.
     */
    void onKeyPressed(KeyEvent event);

    /**
     * Called when a key is released.
     */
    void onKeyReleased(KeyEvent event);

    /**
     * Called when a key is typed (press + release).
     */
    default void onKeyTyped(KeyEvent event) {}

    /**
     * Called when a mouse button is pressed.
     */
    void onMousePressed(MouseEvent event);

    /**
     * Called when a mouse button is released.
     */
    default void onMouseReleased(MouseEvent event) {}

    /**
     * Called when the mouse is moved (no buttons pressed).
     */
    void onMouseMoved(MouseEvent event);

    /**
     * Called when the mouse is dragged (button held down).
     */
    void onMouseDragged(MouseEvent event);

    /**
     * Called when the mouse wheel is scrolled.
     */
    void onMouseWheel(MouseEvent event);

    /**
     * Called when the component is resized.
     */
    void onComponentResized(ComponentEvent event);
}
