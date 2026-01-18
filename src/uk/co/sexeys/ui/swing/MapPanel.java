package uk.co.sexeys.ui.swing;

import uk.co.sexeys.*;
import uk.co.sexeys.rendering.*;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.wind.Wind;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Modern JPanel for map display that uses the Renderer abstraction.
 * This class demonstrates the refactored architecture where:
 * - Business logic is delegated to ApplicationContext/services
 * - Rendering uses the Renderer abstraction
 * - State management is cleaner
 */
public class MapPanel extends JPanel {

    // Services
    private final ApplicationContext appContext;
    private final SwingRenderer renderer;

    // Display state
    private Mercator screen;
    private BufferedImage offscreenBuffer;
    private Calendar displayTime;

    // UI state
    private int lastMouseX, lastMouseY;
    private int dragStartX, dragStartY;
    private boolean isDragging = false;

    // Display options
    private boolean showWind = true;
    private boolean showWater = true;
    private boolean showWaves = true;

    private final SimpleDateFormat dateFormat;

    public MapPanel(ApplicationContext appContext) {
        this.appContext = appContext;
        this.renderer = new SwingRenderer(this);
        this.displayTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        this.dateFormat = new SimpleDateFormat("EEE yyyy.MM.dd HH:mm:ss zzz");
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        setFocusable(true);
        setupEventListeners();
    }

    /**
     * Initialize the screen projection.
     */
    public void initScreen(float lat1, float lon1, float lon2) {
        screen = new Mercator(lat1, lon1, lon2);
        screen.width = getWidth() > 0 ? getWidth() : 1000;
        screen.height = getHeight() > 0 ? getHeight() : 600;
        screen.x2 = new int[]{screen.width};
        screen.y2 = new int[]{screen.height};
        screen.computeParameters(0);

        offscreenBuffer = new BufferedImage(screen.width, screen.height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Set the display time.
     */
    public void setDisplayTime(long timeMs) {
        displayTime.setTimeInMillis(timeMs);
        updateDisplay();
    }

    /**
     * Get the current display time.
     */
    public long getDisplayTime() {
        return displayTime.getTimeInMillis();
    }

    private void setupEventListeners() {
        // Mouse wheel for zooming and time adjustment
        addMouseWheelListener(e -> {
            if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
                // Shift+wheel = change time
                int delta = e.getWheelRotation() < 0 ? 1 : -1;
                if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
                    displayTime.add(Calendar.HOUR, delta);
                } else {
                    displayTime.add(Calendar.MINUTE, delta);
                }
            } else {
                // Regular wheel = zoom
                zoom(e.getX(), e.getY(), e.getWheelRotation() < 0 ? 1/1.5f : 1.5f);
            }
            updateDisplay();
            repaint();
        });

        // Mouse drag for panning
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                    isDragging = true;
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    handleRightClick(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging && screen != null) {
                    int dx = e.getX() - dragStartX;
                    int dy = e.getY() - dragStartY;
                    pan(dx, dy);
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                    updateDisplay();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                updateDisplay();
                repaint();
            }
        });

        // Keyboard shortcuts
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyPress(e);
            }
        });

        // Window resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (screen != null) {
                    screen.width = getWidth();
                    screen.height = getHeight();
                    screen.x2[0] = screen.width;
                    screen.y2[0] = screen.height;
                    screen.computeParameters(0);
                    offscreenBuffer = new BufferedImage(screen.width, screen.height, BufferedImage.TYPE_INT_ARGB);
                    updateDisplay();
                    repaint();
                }
            }
        });
    }

    private void zoom(int mouseX, int mouseY, float factor) {
        if (screen == null) return;

        Vector2 p = screen.fromPointToLatLng(mouseX, mouseY);
        float dlon = screen.lon2 - screen.lon1;
        float dlat = screen.lat2 - screen.lat1;
        screen.lon1 -= (factor - 1) * (p.x - screen.lon1);
        screen.lat1 -= (factor - 1) * (p.y - screen.lat1);
        screen.lon2 = screen.lon1 + dlon * factor;
        screen.lat2 = screen.lat1 + dlat * factor;
        screen.computeParameters(0);
    }

    private void pan(int dx, int dy) {
        if (screen == null) return;

        // Convert pixel delta to geographic delta
        Vector2 origin = screen.fromPointToLatLng(0, 0);
        Vector2 delta = screen.fromPointToLatLng(-dx, -dy);
        float dlat = (float)(delta.y - origin.y);
        float dlon = (float)(delta.x - origin.x);

        screen.lon1 += dlon;
        screen.lon2 += dlon;
        screen.lat1 += dlat;
        screen.lat2 += dlat;
        screen.computeParameters(0);
    }

    private void handleRightClick(MouseEvent e) {
        // Toggle chart visibility at click location
        if (screen == null) return;
        Vector2 geoPos = screen.fromPointToLatLng(e.getX(), e.getY());
        appContext.getChartService().toggleChartAt(geoPos, screen.scale);
        updateDisplay();
        repaint();
    }

    private void handleKeyPress(KeyEvent e) {
        switch (e.getKeyChar()) {
            case 'X' -> {
                showWind = !showWind;
                updateDisplay();
                repaint();
            }
            case 'W' -> {
                showWater = !showWater;
                updateDisplay();
                repaint();
            }
            case '?' -> showHelp();
        }
    }

    private void showHelp() {
        String helpText = """
            Mouse Controls:
               Left drag - Pan map
               Wheel - Zoom
               Shift+Wheel - Change time (minute)
               Ctrl+Shift+Wheel - Change time (hour)
               Right click - Toggle chart

            Keyboard:
               X - Toggle wind display
               W - Toggle water/tide display
               ? - Show this help
            """;
        JOptionPane.showMessageDialog(this, helpText, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Update the offscreen buffer with current state.
     */
    public void updateDisplay() {
        if (offscreenBuffer == null || screen == null) return;

        Graphics2D g2d = offscreenBuffer.createGraphics();
        renderer.setGraphics(g2d, screen.width, screen.height);

        // Clear background
        renderer.setColor(255, 255, 255);
        renderer.fillRect(0, 0, screen.width, screen.height);

        // Create projection adapter
        Projection projection = new MercatorProjection(screen);
        long time = displayTime.getTimeInMillis();

        // Draw layers in order
        drawCharts(projection, time);
        drawWeatherOverlays(projection, time);
        drawRoute(projection, time);
        drawStatusOverlay(g2d);

        g2d.dispose();
    }

    private void drawCharts(Projection projection, long time) {
        ChartService chartService = appContext.getChartService();

        // Draw depth
        // (delegated to Depth.draw when implemented with Renderable)

        // Draw charts
        for (Chart chart : chartService.getCharts()) {
            if (chart.enabled && chart.visibleAtThisScale(screen.scale)) {
                // Chart drawing logic would use renderer
                // For now, delegate to existing draw method
            }
        }

        // Draw tidal streams
        for (TidalStream ts : chartService.getTidalStreams()) {
            if (ts.enabled) {
                // Tidal stream drawing logic
            }
        }
    }

    private void drawWeatherOverlays(Projection projection, long time) {
        WeatherService weatherService = appContext.getWeatherService();

        if (showWind && weatherService.getWind() != null) {
            // Draw wind arrows
            drawWindOverlay(weatherService.getWind(), projection, time);
        }

        if (showWater && weatherService.getWater() != null) {
            // Draw tide/current arrows
            drawWaterOverlay(weatherService.getWater(), projection, time);
        }

        if (showWaves && weatherService.getWaves() != null) {
            // Draw wave height warnings
            drawWaveOverlay(weatherService.getWaves(), projection, time);
        }
    }

    private void drawWindOverlay(Wind wind, Projection projection, long time) {
        // Draw wind arrows at grid points
        double dx = (screen.bottomRight.x - screen.topLeft.x) / 20;
        double dy = (screen.topLeft.y - screen.bottomRight.y) / 20;

        renderer.setColor(Colors.WIND_ARROW);
        Vector2 v = new Vector2();

        for (double lon = screen.topLeft.x + dx; lon < screen.bottomRight.x; lon += dx) {
            for (double lat = screen.bottomRight.y + dy; lat < screen.topLeft.y; lat += dy) {
                Vector2 pos = new Vector2((float)lon, (float)lat);
                wind.getValue(pos, time, v);

                if (v.mag2() > 0.1) {
                    Vector2 screenPos = projection.fromLatLngToPoint(lat, lon);
                    drawWindArrow(screenPos.x, screenPos.y, v);
                }
            }
        }
    }

    private void drawWindArrow(float x, float y, Vector2 wind) {
        // Convert wind vector to screen arrow
        float scale = 3.0f; // pixels per m/s
        float dx = wind.x * scale;
        float dy = -wind.y * scale; // Screen Y is inverted

        renderer.drawLine(x, y, x + dx, y + dy);
        // Add arrowhead
        // (simplified - full implementation would rotate properly)
    }

    private void drawWaterOverlay(Water water, Projection projection, long time) {
        // Similar to wind but with different color
        renderer.setColor(Colors.TIDE_ARROW);
        // ... implementation similar to wind
    }

    private void drawWaveOverlay(Waves waves, Projection projection, long time) {
        // Draw red squares where wave height exceeds warning threshold
        double dx = (screen.bottomRight.x - screen.topLeft.x) / 20;
        double dy = (screen.topLeft.y - screen.bottomRight.y) / 20;

        renderer.setColor(Colors.WAVE_WARNING);

        for (double lon = screen.topLeft.x + dx; lon < screen.bottomRight.x; lon += dx) {
            for (double lat = screen.bottomRight.y + dy; lat < screen.topLeft.y; lat += dy) {
                float height = waves.getValue(new Vector2((float)lon, (float)lat), time);
                if (height > Main.waveWarning) {
                    Vector2 screenPos = projection.fromLatLngToPoint(lat, lon);
                    int size = 30;
                    renderer.fillRect(screenPos.x - size/2f, screenPos.y - size/2f, size, size);
                }
            }
        }
    }

    private void drawRoute(Projection projection, long time) {
        RoutingService routingService = appContext.getRoutingService();
        if (routingService == null) return;

        // Draw JIM isochrones
        // Draw DE route
        // Draw waypoints
        // (Would delegate to Renderable implementations)
    }

    private void drawStatusOverlay(Graphics2D g2d) {
        if (screen == null) return;

        renderer.setColor(Colors.BLACK);
        renderer.setFont("Arial", uk.co.sexeys.rendering.Renderer.FONT_BOLD, Main.fontSize);

        // Mouse position info
        Vector2 geoPos = screen.fromPointToLatLng(lastMouseX, lastMouseY);
        Fix screenFix = new Fix();
        screenFix.position = geoPos.scale(phys.radiansPerDegree);

        StringBuilder sb = new StringBuilder();
        java.util.Formatter formatter = new java.util.Formatter(sb, Locale.UK);
        formatter.format("%s %s %s",
            screenFix.DMSLatitude(),
            screenFix.DMSLongitude(),
            dateFormat.format(displayTime.getTime()));

        renderer.drawText(sb.toString(), 20, Main.fontSize);

        // Weather info at mouse position
        WeatherService weather = appContext.getWeatherService();
        if (weather.getWind() != null) {
            Vector2 wind = new Vector2();
            Wind.SOURCE source = weather.getWind(geoPos, displayTime.getTimeInMillis(), wind);
            sb.setLength(0);
            formatter.format("%s: %.1f m/s E %.1f m/s N", Wind.Source(source), wind.x, wind.y);
            renderer.drawText(sb.toString(), 20, 3 * Main.fontSize);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (offscreenBuffer != null) {
            g.drawImage(offscreenBuffer, 0, 0, null);
        }
    }

    // Public accessors

    public Mercator getScreen() {
        return screen;
    }

    public SwingRenderer getRenderer() {
        return renderer;
    }

    public void setShowWind(boolean show) {
        this.showWind = show;
        updateDisplay();
        repaint();
    }

    public void setShowWater(boolean show) {
        this.showWater = show;
        updateDisplay();
        repaint();
    }

    public void setShowWaves(boolean show) {
        this.showWaves = show;
        updateDisplay();
        repaint();
    }
}
