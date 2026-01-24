package uk.co.sexeys.input;

/**
 * Interface for the view layer that the InputController can call back to.
 * This abstracts UI-specific operations like repainting, cursor changes, and dialogs.
 */
public interface ChartView {

    /**
     * Request the view to repaint/redraw.
     */
    void requestRepaint();

    /**
     * Request the view to update its graphics buffer.
     */
    void updateGraphics();

    /**
     * Set the cursor to indicate waiting/busy state.
     */
    void setWaitCursor();

    /**
     * Set the cursor to default state.
     */
    void setDefaultCursor();

    /**
     * Set the cursor to indicate move/pan operation.
     */
    void setMoveCursor();

    /**
     * Show an information dialog with the given title and message.
     */
    void showInfoDialog(String title, String message);

    /**
     * Show an error dialog with the given title and message.
     */
    void showErrorDialog(String title, String message);

    /**
     * Copy text to the system clipboard.
     */
    void copyToClipboard(String text);

    /**
     * Get the current display width.
     */
    int getDisplayWidth();

    /**
     * Get the current display height.
     */
    int getDisplayHeight();

    /**
     * Get the current scaling factor (for HiDPI displays).
     */
    double getScaling();
}
