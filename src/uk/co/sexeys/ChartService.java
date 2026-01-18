package uk.co.sexeys;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing chart data including raster charts, tidal streams, and CMap data.
 */
public class ChartService {

    private final List<Chart> charts;
    private final List<TidalStream> tidalStreams;
    private WVS wvs;
    private Depth depth;
    private IDX idx;
    private Harmonics harmonics;

    public ChartService() {
        this.charts = new ArrayList<>();
        this.tidalStreams = new ArrayList<>();
    }

    /**
     * Load World Vector Shoreline data.
     */
    public void loadWVS(int resolution) {
        wvs = new WVS(resolution);
    }

    /**
     * Set WVS scale.
     */
    public void setWVSScale(float scale) throws IOException {
        if (wvs != null) {
            wvs.scale(scale);
        }
    }

    /**
     * Load depth data from GEBCO file.
     */
    public void loadDepth(String path, int scale) {
        depth = new Depth(path, scale);
    }

    /**
     * Load tidal harmonics data.
     */
    public void loadTidalHarmonics(String basePath) {
        idx = new IDX(basePath);
        harmonics = new Harmonics(basePath);
        TidalStream.harmonics = harmonics;
        TidalStream.idx = idx;
    }

    /**
     * Read IDX entries.
     */
    public void readIDX() throws IOException {
        if (idx != null) {
            idx.read();
        }
    }

    /**
     * Add charts from a directory with Charts.txt metadata.
     */
    public void addCharts(String directory) {
        Chart.addTable(directory, (ArrayList<Chart>) charts);
    }

    /**
     * Reload charts.
     */
    public void reloadCharts(String directory) {
        charts.clear();
        Chart.addTable(directory, (ArrayList<Chart>) charts);
    }

    /**
     * Add a tidal stream chart.
     */
    public void addTidalStream(String file, int[] x1, int[] y1, int[] x2, int[] y2,
                               float lat1, float lon1, float lat2, float lon2, String station) {
        tidalStreams.add(new TidalStream(file, x1, y1, x2, y2, lat1, lon1, lat2, lon2, station));
    }

    /**
     * Toggle chart visibility at a screen position.
     *
     * @return true if a chart was toggled
     */
    public boolean toggleChartAt(Vector2 geoPosition, float screenScale) {
        Chart clicked = null;
        for (Chart c : charts) {
            if (screenScale > 2 * c.scale) continue;
            if (screenScale < 0.05 * c.scale) continue;
            if (c.contains(geoPosition)) {
                if (clicked == null || c.scale < clicked.scale) {
                    clicked = c;
                }
            }
        }
        if (clicked != null) {
            clicked.toggleVisibility();
            return true;
        }
        return false;
    }

    /**
     * Toggle tidal stream visibility at a screen position.
     *
     * @return true if a tidal stream was toggled
     */
    public boolean toggleTidalStreamAt(Vector2 geoPosition) {
        List<TidalStream> clicked = new ArrayList<>();
        for (TidalStream ts : tidalStreams) {
            if (ts.contains(geoPosition)) {
                clicked.add(ts);
            }
        }
        for (TidalStream c : clicked) {
            boolean toggle = true;
            for (TidalStream d : clicked) {
                if (c == d) continue;
                if (c.contains(d)) {
                    toggle = false;
                    break;
                }
            }
            if (toggle) {
                c.toggleVisibility();
            }
        }
        return !clicked.isEmpty();
    }

    /**
     * Find closest tidal station to a screen position.
     */
    public IDX.IDX_entry findClosestStation(Vector2 screenPosition) {
        if (idx != null) {
            return idx.closestDrawn(screenPosition);
        }
        return null;
    }

    // Getters for rendering access

    public List<Chart> getCharts() {
        return charts;
    }

    public List<TidalStream> getTidalStreams() {
        return tidalStreams;
    }

    public WVS getWvs() {
        return wvs;
    }

    public Depth getDepth() {
        return depth;
    }

    public IDX getIdx() {
        return idx;
    }

    public Harmonics getHarmonics() {
        return harmonics;
    }
}
