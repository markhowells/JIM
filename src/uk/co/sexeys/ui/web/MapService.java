package uk.co.sexeys.ui.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.sexeys.*;
import uk.co.sexeys.CMap.CMap;
import uk.co.sexeys.JIM.Agent;
import uk.co.sexeys.JIM.CrossTrack;
import uk.co.sexeys.JIM.JIM;
import uk.co.sexeys.rendering.MercatorProjection;
import uk.co.sexeys.water.Current;
import uk.co.sexeys.water.PrevailingCurrent;
import uk.co.sexeys.water.Tide;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.Depart;
import uk.co.sexeys.waypoint.Waypoint;
import uk.co.sexeys.wind.Prevailing;
import uk.co.sexeys.wind.SailDocs;
import uk.co.sexeys.wind.Wind;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service that manages the map state and rendering for web clients.
 * This is a singleton service that holds the shared map state.
 */
@Service
public class MapService {
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);

    private Mercator screen;
    private MercatorProjection projection;
    private Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private final SimpleDateFormat format = new SimpleDateFormat("EEE yyyy.MM.dd HH:mm:ss zzz");

    // Data layers
    private WVS wvs;
    private CMap cMap;
    private Wind wind;
    private Water water;
    private Waves waves;
    private Boat boat;
    private JIM jim;
    private IDX idx;
    private Harmonics harmonics;
    private Depth depth;
    private LastRoute lastRoute;

    // Visibility toggles
    private boolean showWind = true;
    private boolean showWater = true;
    private boolean showWaves = true;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Initialize the map service with configuration.
     * Called lazily on first render request.
     */
    public synchronized void initialize() {
        if (initialized) return;

        try {
            // Load configuration
            Config.load();
            initFromConfig();

            // Initialize shoreline
            Main.shoreline = new Shoreline(Main.WVSResolution);

            // Create boat and parse route
            boat = new Boat();
            boat.waypoints = parseRoute(Main.ROUTE);

            // Load polars
            loadPolars();

            // Initialize screen
            String[] temp = Main.SCREEN.split(" ");
            float screenTop = Fix.parseLatitude(temp[0]);
            float screenLeft = Fix.parseLongitude(temp[1]);
            float screenRight = Fix.parseLongitude(temp[3]);
            screen = new Mercator(screenTop, screenLeft, screenRight);
            screen.width = 1200;
            screen.height = 800;
            // Set x2/y2 to screen dimensions - computeParameters uses these
            screen.x2 = new int[]{screen.width};
            screen.y2 = new int[]{screen.height};
            // Calculate lat2 from the aspect ratio (lon range / width * height)
            float lonRange = screenRight - screenLeft;
            float latRange = lonRange * screen.height / screen.width;
            screen.lat2 = screenTop - latRange;
            screen.computeParameters(0);
            screen.enabled = true;

            projection = new MercatorProjection(screen);

            // Initialize data layers
            wvs = new WVS(Main.WVSResolution);
            cMap = new CMap(0);
            idx = new IDX(Main.root + File.separator + "charts/tides/HARMONIC");
            harmonics = new Harmonics(Main.root + File.separator + "charts/tides/HARMONIC");
            depth = new Depth(Main.root + File.separator + "charts/Bathymetry/GEBCO_2020.dat", 4);

            try {
                idx.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Compute initial route
            computeRoute();

            // Load last route if available
            lastRoute = new LastRoute();
            logger.info("LastRoute loaded with {} waypoints", lastRoute.waypoints.size());

            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize MapService", e);
        }
    }

    private void initFromConfig() {
        Config cfg = Config.get();
        Main.SCREEN = cfg.getScreen();
        Main.ROUTE = cfg.getRoute();
        Main.root = cfg.getRoot();
        logger.info("MapService config loaded:");
        logger.info("  Main.root = {}", Main.root);
        logger.info("  Prevailing path would be: {}", Main.root + "Prevailing" + File.separator + "prevailingCurrents.nc");
        java.io.File prevFile = new java.io.File(Main.root + "Prevailing" + File.separator + "prevailingCurrents.nc");
        logger.info("  File exists: {}, absolute path: {}", prevFile.exists(), prevFile.getAbsolutePath());
        Main.WindSource = cfg.getWindSource();
        Main.WindResolution = cfg.getWindResolution();
        Main.prevailingTransitionPeriod = cfg.getPrevailingTransitionPeriod();
        Main.sparsePolar = cfg.isSparsePolar();
        Main.polarHighWindOnly = cfg.isPolarHighWindOnly();
        Main.useWater = cfg.isUseWater();
        Main.crossDateLine = cfg.isCrossDateLine();
        Main.useIceZone = cfg.isUseIceZone();
        Main.WVSResolution = cfg.getWvsResolution();
        Main.ChartOffsetY = cfg.getChartOffsetY();
        Main.ChartOffsetX = cfg.getChartOffsetX();
        Main.REPLAY = cfg.getReplay();
        Main.ExpandingTimeFactor = cfg.getExpandingTimeFactor();
        Main.minLon = cfg.getMinLon();
        Main.maxLon = cfg.getMaxLon();
        Main.numberOfFixes = cfg.getNumberOfFixes();
        Main.searchTolerance = cfg.getSearchTolerance();
        Main.C2SSearchPeriod = cfg.getC2sSearchPeriod();
        Main.C2SLegs = cfg.getC2sLegs();
        Main.C2SAgents = cfg.getC2sAgents();
        Main.C2SCR = cfg.getC2sCR();
        Main.routeAspectRatio = cfg.getRouteAspectRatio();
        Main.continuousFactor = cfg.getContinuousFactor();
        Main.ShowRouteResolution = cfg.isShowRouteResolution();
        Main.useDifferentialEvolution = cfg.isUseDifferentialEvolution();
        Main.JIMCutoff = cfg.getJimCutoff();
        Main.waveWarning = cfg.getWaveWarning();
        Main.fontSize = cfg.getFontSize();
    }

    private Waypoint[] parseRoute(String routeString) {
        // Route parsing - matches StreamFrame.ParseRoute()
        String[] lines = routeString.split("\n");
        LinkedList<Waypoint> waypoints = new LinkedList<>();
        LinkedList<Obstruction> obstructions = null;
        List<String> windList = new LinkedList<>();
        List<String> tideList = new LinkedList<>();
        List<String> currentList = new LinkedList<>();
        List<String> wavesList = new LinkedList<>();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("#")) continue;

            if (trimmedLine.startsWith("Using Polar:")) {
                // Polar handled separately in loadPolars()
            } else if (trimmedLine.startsWith("Using Wind:")) {
                String[] temp = trimmedLine.split("Using Wind: ");
                if (temp.length > 1) {
                    windList.add(Main.root + "grib" + File.separator + temp[1]);
                    logger.info("Added wind file: {}", Main.root + "grib" + File.separator + temp[1]);
                }
            } else if (trimmedLine.startsWith("Using Tide:")) {
                String[] temp = trimmedLine.split("Using Tide: ");
                if (temp.length > 1) {
                    tideList.add(Main.root + "grib" + File.separator + temp[1]);
                    logger.info("Added tide file: {}", Main.root + "grib" + File.separator + temp[1]);
                }
            } else if (trimmedLine.startsWith("Using Current:")) {
                String[] temp = trimmedLine.split("Using Current: ");
                if (temp.length > 1) {
                    currentList.add(Main.root + "grib" + File.separator + temp[1]);
                    logger.info("Added current file: {}", Main.root + "grib" + File.separator + temp[1]);
                }
            } else if (trimmedLine.startsWith("Using Waves:")) {
                String[] temp = trimmedLine.split("Using Waves: ");
                if (temp.length > 1) {
                    wavesList.add(Main.root + "grib" + File.separator + temp[1]);
                    logger.info("Added waves file: {}", Main.root + "grib" + File.separator + temp[1]);
                }
            } else if (trimmedLine.startsWith("Depart")) {
                waypoints.add(new Depart(trimmedLine));
                obstructions = new LinkedList<>();
            } else if (trimmedLine.startsWith("Expand")) {
                uk.co.sexeys.waypoint.Waypoint previous = waypoints.getLast();
                waypoints.add(new uk.co.sexeys.waypoint.Expand(trimmedLine, obstructions, previous));
                obstructions = new LinkedList<>();
            } else if (trimmedLine.startsWith("Destination")) {
                uk.co.sexeys.waypoint.Waypoint previous = waypoints.getLast();
                waypoints.add(new uk.co.sexeys.waypoint.Destination(trimmedLine, obstructions, previous));
                obstructions = new LinkedList<>();
            } else if (trimmedLine.startsWith("Obstruction")) {
                obstructions.add(new Obstruction(trimmedLine));
            }
        }

        if (!waypoints.isEmpty() && waypoints.getFirst() instanceof Depart) {
            long departTime = ((Depart) waypoints.getFirst()).time;
            UTC.setTimeInMillis(departTime);

            // Initialize wind - start with Prevailing, then layer GRIB data on top
            wind = new Prevailing(departTime);
            if (!windList.isEmpty()) {
                logger.info("Loading {} wind GRIB files", windList.size());
                wind = new SailDocs(windList, wind);
            }

            // Initialize water/current - start with Prevailing, then layer GRIB data
            water = new PrevailingCurrent(departTime);
            if (!currentList.isEmpty()) {
                logger.info("Loading {} current GRIB files", currentList.size());
                water = new Current(currentList, water);
            }
            if (!tideList.isEmpty()) {
                logger.info("Loading {} tide files", tideList.size());
                water = new Tide(tideList, water);
            }

            // Initialize waves
            if (!wavesList.isEmpty()) {
                logger.info("Loading {} wave GRIB files", wavesList.size());
                waves = new Waves(wavesList);
            } else {
                waves = new Waves(departTime);
            }
        }

        return waypoints.toArray(new Waypoint[0]);
    }

    private void loadPolars() {
        // Find polar directory from route config
        String polarPath = Main.root;  // Default
        String[] lines = Main.ROUTE.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("Using Polar:")) {
                polarPath = Main.root + line.split("Using Polar:")[1].trim();
                break;
            }
        }

        File polarDir = new File(polarPath);
        if (!polarDir.exists() || !polarDir.isDirectory()) {
            System.err.println("Polar directory not found: " + polarPath);
            boat.polar = new Polar();
            return;
        }

        boat.polar = new Polar();
        File[] files = polarDir.listFiles();
        if (files == null) return;

        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            try {
                sb.setLength(0);
                FileInputStream is = new FileInputStream(file);
                byte[] b = new byte[is.available()];
                is.read(b);
                is.close();
                sb.append(new String(b));
                sb.append("\n");
                boat.polar.ScanVirtualRegattaPolar(sb.toString(), file.getName());
            } catch (IOException e) {
                System.err.println("Error reading polar: " + file.getName());
            }
        }
        boat.polar.combinePolars();
        boat.polar.computeVMGPolar();
    }

    private void computeRoute() {
        try {
            logger.info("Computing route...");
            logger.info("  boat.waypoints.length = {}", boat.waypoints.length);
            for (int i = 0; i < boat.waypoints.length; i++) {
                logger.info("  waypoint[{}]: {} at ({}, {}) binWidth={} numberOfBins={} timeStep={}", i,
                    boat.waypoints[i].getClass().getSimpleName(),
                    Math.toDegrees(boat.waypoints[i].position.x),
                    Math.toDegrees(boat.waypoints[i].position.y),
                    boat.waypoints[i].binWidth,
                    boat.waypoints[i].numberOfBins,
                    boat.waypoints[i].timeStep);
            }
            logger.info("  boat.polar = {}", boat.polar);
            logger.info("  boat.polar.raw = {}", boat.polar != null ? boat.polar.raw : "null");
            logger.info("  wind = {}", wind);
            logger.info("  water = {}", water);
            jim = new CrossTrack(boat, wind, water);
            jim.SearchInit();
            logger.info("  SearchInit complete, route.currentTime = {}", jim.route.currentTime);
            logger.info("  Main.JIMCutoff = {} ms ({} hours)", Main.JIMCutoff, Main.JIMCutoff / 3600000.0);
            jim.Search(jim.route.currentTime + Main.JIMCutoff);
            logger.info("  Search complete, keyAgent = {}", jim.keyAgent);
            if (jim.keyAgent != null) {
                logger.info("  keyAgent.position = ({}, {})",
                    Math.toDegrees(jim.keyAgent.position.x),
                    Math.toDegrees(jim.keyAgent.position.y));
                // Count agent chain length
                int chainLength = 0;
                Agent a = jim.keyAgent;
                while (a != null) {
                    chainLength++;
                    a = a.previousAgent;
                }
                logger.info("  keyAgent chain length = {}", chainLength);
            }
            Obstruction.active = boat.waypoints[1].obstructions;
        } catch (Exception e) {
            logger.error("Error computing route", e);
            e.printStackTrace();
        }
    }

    /**
     * Render the current map state to drawing commands.
     */
    public MapController.RenderResponse render(int width, int height) {
        if (!initialized) {
            initialize();
        }

        // Update screen size if changed
        if (screen.width != width || screen.height != height) {
            screen.width = width;
            screen.height = height;
            screen.x2 = new int[]{width};
            screen.y2 = new int[]{height};
            // Recalculate lat2 to maintain aspect ratio
            float lonRange = screen.lon2 - screen.lon1;
            float latRange = lonRange * height / width;
            screen.lat2 = screen.lat1 - latRange;
            screen.computeParameters(0);
            projection = new MercatorProjection(screen);
            cMap.update(screen);
        }

        WebRenderer renderer = new WebRenderer(width, height);
        renderer.clear();

        long time = UTC.getTimeInMillis();

        // Render layers
        if (depth != null && cMap.scaleLevel < 0) {
            depth.render(renderer, projection, time);
        }

        if (showWaves && waves != null) {
            waves.render(renderer, projection, time);
        }

        cMap.render(renderer, projection, time);
        wvs.render(renderer, projection, time);

        if (showWind && wind != null) {
            wind.render(renderer, projection, time);
        }

        if (Main.useWater && showWater && water != null) {
            water.render(renderer, projection, time);
        }

        // Render waypoints
        for (Waypoint w : boat.waypoints) {
            w.render(renderer, projection, time);
        }

        // Render JIM route
        if (jim != null) {
            jim.render(renderer, projection, time);
        }

        // Render last route
        if (lastRoute != null) {
            lastRoute.render(renderer, projection, time);
        }

        // Render boat
        boat.render(renderer, projection, time);

        MapController.ViewportInfo viewport = getViewport();
        return new MapController.RenderResponse(renderer.getCommands(), time, viewport);
    }

    /**
     * Pan the map by screen coordinate deltas.
     */
    public void pan(double deltaX, double deltaY) {
        if (!initialized) return;

        // Convert screen delta to geographic delta
        Vector2 p1 = screen.fromPointToLatLng(0, 0).scale(phys.radiansPerDegree);
        Vector2 p2 = screen.fromPointToLatLng((float) deltaX, (float) deltaY).scale(phys.radiansPerDegree);
        Vector2 diff = p2.minus(p1);
        diff.scaleIP((float) (180 / Math.PI));

        screen.lon1 -= diff.x;
        screen.lat1 -= diff.y;
        screen.lon2 -= diff.x;
        screen.lat2 -= diff.y;
        screen.computeParameters(0);
        cMap.update(screen);
        projection = new MercatorProjection(screen);
    }

    /**
     * Zoom the map by a factor centered on a screen point.
     */
    public void zoom(double factor, double centerX, double centerY) {
        if (!initialized) return;

        Vector2 p = screen.fromPointToLatLng((float) centerX, (float) centerY);

        float dlon = screen.lon2 - screen.lon1;
        float dlat = screen.lat2 - screen.lat1;
        screen.lon1 -= (float) ((factor - 1) * (p.x - screen.lon1));
        screen.lat1 -= (float) ((factor - 1) * (p.y - screen.lat1));
        screen.lon2 = screen.lon1 + dlon * (float) factor;
        screen.lat2 = screen.lat1 + dlat * (float) factor;
        screen.computeParameters(0);
        cMap.update(screen);
        projection = new MercatorProjection(screen);
    }

    /**
     * Set the display time.
     */
    public void setTime(long timestamp) {
        UTC.setTimeInMillis(timestamp);
    }

    /**
     * Adjust the display time by minutes.
     */
    public void adjustTime(int deltaMinutes) {
        UTC.add(Calendar.MINUTE, deltaMinutes);
    }

    /**
     * Handle a click on the map.
     */
    public MapController.ClickResponse handleClick(double x, double y, int button, List<String> modifiers) {
        if (!initialized) return new MapController.ClickResponse("none", Map.of());

        Vector2 p = screen.fromPointToLatLng((float) x, (float) y);

        if (button == 3) {
            // Right click - could toggle charts
            return new MapController.ClickResponse("context_menu", Map.of(
                "lat", p.y,
                "lon", p.x
            ));
        }

        return new MapController.ClickResponse("click", Map.of(
            "lat", p.y,
            "lon", p.x
        ));
    }

    /**
     * Handle hover for info display.
     */
    public MapController.HoverResponse handleHover(double x, double y) {
        MapController.HoverResponse response = new MapController.HoverResponse();

        if (!initialized) return response;

        Vector2 screenPos = screen.fromPointToLatLng((float) x, (float) y);
        Vector2 pos = new Vector2(screenPos).scale(phys.radiansPerDegree);

        Fix fix = new Fix();
        fix.position = pos;
        response.position = fix.DMSLatitude() + " " + fix.DMSLongitude();

        try {
            if (wind != null) {
                Vector2 w = new Vector2();
                wind.getValue(pos.scale(phys.degrees), UTC.getTimeInMillis(), w);
                response.wind = String.format("%.1f m/s E, %.1f m/s N", w.x, w.y);
            }
        } catch (Exception e) {
            response.wind = "N/A";
        }

        try {
            if (water != null) {
                Vector2 t = new Vector2();
                Vector2 queryPos = pos.scale(phys.degrees);
                water.getValue(queryPos, UTC.getTimeInMillis(), t);
                response.tide = String.format("%.3f m/s E, %.3f m/s N", t.x, t.y);
                // Debug: log all hover calls
                logger.info("Tide hover query: queryPos=({},{}), result=({},{})", queryPos.x, queryPos.y, t.x, t.y);
            } else {
                logger.warn("water is null!");
                response.tide = "N/A (no water data)";
            }
        } catch (Exception e) {
            response.tide = "N/A";
            logger.error("Error getting tide data", e);
        }

        try {
            if (waves != null) {
                float height = waves.getValue(pos.scale(phys.degrees), UTC.getTimeInMillis());
                // Check for missing/undefined values (GRIB uses 99999e20 as undefined, -1 means out of bounds)
                if (height < 0 || height > 1e10 || Float.isNaN(height) || Float.isInfinite(height)) {
                    response.waves = "N/A";
                } else {
                    response.waves = String.format("%.1f m", height);
                }
            }
        } catch (Exception e) {
            response.waves = "N/A";
        }

        return response;
    }

    /**
     * Get current viewport information.
     */
    public MapController.ViewportInfo getViewport() {
        if (!initialized || screen == null) {
            return new MapController.ViewportInfo(0, 0, 0, 0, 0);
        }
        return new MapController.ViewportInfo(
            screen.lat1,
            screen.lat2,
            screen.lon2,
            screen.lon1,
            screen.scale
        );
    }

    /**
     * Toggle layer visibility.
     */
    public void toggleLayer(String layer) {
        switch (layer.toLowerCase()) {
            case "wind" -> showWind = !showWind;
            case "water", "tide" -> showWater = !showWater;
            case "waves" -> showWaves = !showWaves;
        }
    }

    /**
     * Get current time info.
     */
    public MapController.TimeInfo getTimeInfo() {
        return new MapController.TimeInfo(
            UTC.getTimeInMillis(),
            format.format(UTC.getTime())
        );
    }
}
