package uk.co.sexeys;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Configuration loader for JIM.
 * Loads settings from a YAML configuration file.
 */
public class Config {
    private static Config instance;
    private final Map<String, Object> config;
    private final String configPath;

    // Cached values for frequently accessed settings
    private String screen;
    private String route;
    private String root;
    private String windSource;
    private String windResolution;
    private long prevailingTransitionPeriod;
    private boolean sparsePolar;
    private boolean polarHighWindOnly;
    private boolean useWater;
    private boolean crossDateLine;
    private boolean useIceZone;
    private int wvsResolution;
    private float chartOffsetX;
    private float chartOffsetY;
    private String replay;
    private long expandingTimeFactor;
    private int minLon;
    private int maxLon;
    private int numberOfFixes;
    private float searchTolerance;
    private long c2sSearchPeriod;
    private int c2sLegs;
    private int c2sAgents;
    private float c2sCR;
    private float routeAspectRatio;
    private float continuousFactor;
    private boolean showRouteResolution;
    private boolean useDifferentialEvolution;
    private long jimCutoff;
    private float waveWarning;
    private int fontSize;

    private Config(String configPath) throws FileNotFoundException {
        this.configPath = configPath;
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(configPath);
        this.config = yaml.load(inputStream);
        parseConfig();
    }

    /**
     * Load configuration from file. Call this once at startup.
     * @param configPath Path to the YAML configuration file
     */
    public static void load(String configPath) throws FileNotFoundException {
        instance = new Config(configPath);
    }

    /**
     * Load configuration from default location (config.yaml in current directory).
     */
    public static void load() throws FileNotFoundException {
        load("config.yaml");
    }

    /**
     * Get the singleton configuration instance.
     */
    public static Config get() {
        if (instance == null) {
            throw new IllegalStateException("Configuration not loaded. Call Config.load() first.");
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private void parseConfig() {
        // Screen
        screen = getString("screen", "53*00'N 11*30'W 37*00'N 0*0'W");

        // Data root
        root = getString("dataRoot", "./database/");

        // Wind settings
        Map<String, Object> wind = getMap("wind");
        windSource = getStringFromMap(wind, "source", "https://nomads.ncep.noaa.gov/pub/data/nccf/com/gfs/prod/");
        windResolution = getStringFromMap(wind, "resolution", "1p00");

        // Features
        Map<String, Object> features = getMap("features");
        useWater = getBooleanFromMap(features, "useWater", true);
        crossDateLine = getBooleanFromMap(features, "crossDateLine", false);
        useIceZone = getBooleanFromMap(features, "useIceZone", false);

        // WVS Resolution
        wvsResolution = getInt("wvsResolution", 100000);

        // Chart offset
        Map<String, Object> chartOffset = getMap("chartOffset");
        chartOffsetX = getFloatFromMap(chartOffset, "x", 0) / 3600f;
        chartOffsetY = getFloatFromMap(chartOffset, "y", 0) / 3600f;

        // Longitude bounds
        Map<String, Object> lonBounds = getMap("longitudeBounds");
        minLon = getIntFromMap(lonBounds, "min", -20);
        maxLon = getIntFromMap(lonBounds, "max", 0);

        // C2S settings
        Map<String, Object> c2s = getMap("c2s");
        numberOfFixes = getIntFromMap(c2s, "numberOfFixes", 80);
        searchTolerance = getFloatFromMap(c2s, "searchTolerance", 100);
        c2sSearchPeriod = (long) (getFloatFromMap(c2s, "searchPeriodHours", 5) * phys.msPerHour);
        c2sLegs = getIntFromMap(c2s, "legs", 8);
        int agentsPerLeg = getIntFromMap(c2s, "agentsPerLeg", 10);
        c2sAgents = agentsPerLeg * c2sLegs;
        c2sCR = getFloatFromMap(c2s, "crossoverProbability", 0.9f);

        // Polar settings
        Map<String, Object> polar = getMap("polar");
        sparsePolar = getBooleanFromMap(polar, "sparse", false);
        polarHighWindOnly = getBooleanFromMap(polar, "highWindOnly", false);

        // Algorithm settings
        Map<String, Object> algorithm = getMap("algorithm");
        expandingTimeFactor = getLongFromMap(algorithm, "expandingTimeFactor", 30);
        routeAspectRatio = getFloatFromMap(algorithm, "routeAspectRatio", 1.0f);
        continuousFactor = getFloatFromMap(algorithm, "continuousFactor", 0.3f);
        showRouteResolution = getBooleanFromMap(algorithm, "showRouteResolution", false);
        useDifferentialEvolution = getBooleanFromMap(algorithm, "useDifferentialEvolution", false);
        jimCutoff = (long) (getFloatFromMap(algorithm, "jimCutoffHours", 100) * phys.msPerHour);
        prevailingTransitionPeriod = (long) (getFloatFromMap(algorithm, "prevailingTransitionDays", 15) * phys.msPerDay);

        // Display settings
        Map<String, Object> display = getMap("display");
        fontSize = getIntFromMap(display, "fontSize", 30);
        waveWarning = getFloatFromMap(display, "waveWarningMeters", 3);

        // Replay
        replay = getString("replay", "");

        // Build route string from structured config
        route = buildRouteString();
    }

    @SuppressWarnings("unchecked")
    private String buildRouteString() {
        Map<String, Object> routeConfig = getMap("route");
        if (routeConfig == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Polar (no space after colon - that's how it's parsed)
        String polar = getStringFromMap(routeConfig, "polar", null);
        if (polar != null) {
            sb.append("Using Polar:").append(polar).append("\n");
        }

        // Using entries (Wind, Tide, Waves, etc.)
        List<Map<String, Object>> using = (List<Map<String, Object>>) routeConfig.get("using");
        if (using != null) {
            for (Map<String, Object> entry : using) {
                String type = (String) entry.get("type");
                String file = (String) entry.get("file");
                sb.append("Using ").append(type).append(": ").append(file).append("\n");
            }
        }

        // Depart
        Map<String, Object> depart = (Map<String, Object>) routeConfig.get("depart");
        if (depart != null) {
            String position = (String) depart.get("position");
            String time = (String) depart.get("time");
            sb.append("Depart: ").append(position).append(" ").append(time).append("\n");
        }

        // Expand - format: "Expand: 1 nm 1000 bins of 4 nm 1 hour step"
        Map<String, Object> expand = (Map<String, Object>) routeConfig.get("expand");
        if (expand != null) {
            String distance = (String) expand.get("distance");
            int bins = getIntFromMap(expand, "bins", 360);
            String binSize = getStringFromMap(expand, "binSize", "1 nm");
            float step = getFloatFromMap(expand, "stepHours", 0.1f);
            sb.append("Expand: ").append(distance).append(" ").append(bins)
              .append(" bins of ").append(binSize).append(" ").append(step).append(" hour step\n");
        }

        // Obstructions
        List<Map<String, Object>> obstructions = (List<Map<String, Object>>) routeConfig.get("obstructions");
        if (obstructions != null) {
            for (Map<String, Object> obs : obstructions) {
                String polygon = (String) obs.get("polygon");
                String name = getStringFromMap(obs, "name", "");
                sb.append("Obstruction: ").append(polygon);
                if (!name.isEmpty()) {
                    sb.append(" ").append(name);
                }
                sb.append("\n");
            }
        }

        // Destination
        Map<String, Object> dest = (Map<String, Object>) routeConfig.get("destination");
        if (dest != null) {
            String position = (String) dest.get("position");
            String radius = (String) dest.get("radius");
            int bins = getIntFromMap(dest, "bins", 360);
            String binSize = (String) dest.get("binSize");
            float step = getFloatFromMap(dest, "stepHours", 0.1f);
            sb.append("Destination: ").append(position).append(" ").append(radius)
              .append(" ").append(bins).append(" bins of ").append(binSize)
              .append(" ").append(step).append(" hour step\n");
        }

        String result = sb.toString();
        System.out.println("Generated route string:\n" + result);
        return result;
    }

    // Helper methods for parsing
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(String key) {
        Object value = config.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Map.of();
    }

    private String getString(String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInt(String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private String getStringFromMap(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean getBooleanFromMap(Map<String, Object> map, String key, boolean defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private int getIntFromMap(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private long getLongFromMap(Map<String, Object> map, String key, long defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private float getFloatFromMap(Map<String, Object> map, String key, float defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }

    // Public getters
    public String getScreen() { return screen; }
    public String getRoute() { return route; }
    public String getRoot() { return root; }
    public String getWindSource() { return windSource; }
    public String getWindResolution() { return windResolution; }
    public long getPrevailingTransitionPeriod() { return prevailingTransitionPeriod; }
    public boolean isSparsePolar() { return sparsePolar; }
    public boolean isPolarHighWindOnly() { return polarHighWindOnly; }
    public boolean isUseWater() { return useWater; }
    public boolean isCrossDateLine() { return crossDateLine; }
    public boolean isUseIceZone() { return useIceZone; }
    public int getWvsResolution() { return wvsResolution; }
    public float getChartOffsetX() { return chartOffsetX; }
    public float getChartOffsetY() { return chartOffsetY; }
    public String getReplay() { return replay; }
    public long getExpandingTimeFactor() { return expandingTimeFactor; }
    public int getMinLon() { return minLon; }
    public int getMaxLon() { return maxLon; }
    public int getNumberOfFixes() { return numberOfFixes; }
    public float getSearchTolerance() { return searchTolerance; }
    public long getC2sSearchPeriod() { return c2sSearchPeriod; }
    public int getC2sLegs() { return c2sLegs; }
    public int getC2sAgents() { return c2sAgents; }
    public float getC2sCR() { return c2sCR; }
    public float getRouteAspectRatio() { return routeAspectRatio; }
    public float getContinuousFactor() { return continuousFactor; }
    public boolean isShowRouteResolution() { return showRouteResolution; }
    public boolean isUseDifferentialEvolution() { return useDifferentialEvolution; }
    public long getJimCutoff() { return jimCutoff; }
    public float getWaveWarning() { return waveWarning; }
    public int getFontSize() { return fontSize; }
    public String getConfigPath() { return configPath; }
}
