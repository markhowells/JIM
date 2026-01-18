package uk.co.sexeys.rendering;

/**
 * Context object that holds rendering state and options.
 * Passed to Renderable objects to control how they render.
 */
public class RenderContext {

    private final Renderer renderer;
    private final Projection projection;
    private final long displayTime;
    private final RenderOptions options;

    public RenderContext(Renderer renderer, Projection projection, long displayTime, RenderOptions options) {
        this.renderer = renderer;
        this.projection = projection;
        this.displayTime = displayTime;
        this.options = options;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public Projection getProjection() {
        return projection;
    }

    public long getDisplayTime() {
        return displayTime;
    }

    public RenderOptions getOptions() {
        return options;
    }

    /**
     * Rendering options that control visibility and display modes.
     */
    public static class RenderOptions {
        private boolean showWind = true;
        private boolean showWater = true;
        private boolean showWaves = true;
        private boolean showCandidates = true;
        private boolean showBestTack = false;
        private boolean showConstantCourse = false;
        private boolean showConstantTWA = false;
        private int fontSize = 14;

        public boolean isShowWind() {
            return showWind;
        }

        public void setShowWind(boolean showWind) {
            this.showWind = showWind;
        }

        public boolean isShowWater() {
            return showWater;
        }

        public void setShowWater(boolean showWater) {
            this.showWater = showWater;
        }

        public boolean isShowWaves() {
            return showWaves;
        }

        public void setShowWaves(boolean showWaves) {
            this.showWaves = showWaves;
        }

        public boolean isShowCandidates() {
            return showCandidates;
        }

        public void setShowCandidates(boolean showCandidates) {
            this.showCandidates = showCandidates;
        }

        public boolean isShowBestTack() {
            return showBestTack;
        }

        public void setShowBestTack(boolean showBestTack) {
            this.showBestTack = showBestTack;
        }

        public boolean isShowConstantCourse() {
            return showConstantCourse;
        }

        public void setShowConstantCourse(boolean showConstantCourse) {
            this.showConstantCourse = showConstantCourse;
        }

        public boolean isShowConstantTWA() {
            return showConstantTWA;
        }

        public void setShowConstantTWA(boolean showConstantTWA) {
            this.showConstantTWA = showConstantTWA;
        }

        public int getFontSize() {
            return fontSize;
        }

        public void setFontSize(int fontSize) {
            this.fontSize = fontSize;
        }
    }
}
