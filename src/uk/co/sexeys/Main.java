package uk.co.sexeys;

import java.io.FileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    // Configuration is loaded from config.yaml (or specified config file)
    // These fields are populated from Config after loading

    // Display and route settings (from config)
    public static String SCREEN;
    public static String ROUTE;
    public static String root;
    public static String WindSource;
    public static String WindResolution;
    public static long prevailingTransitionPeriod;
    public static boolean sparsePolar;
    public static boolean polarHighWindOnly;
    public static boolean useWater;
    public static boolean crossDateLine;
    public static boolean useIceZone;
    public static int WVSResolution;
    public static float ChartOffsetY;
    public static float ChartOffsetX;
    public static String REPLAY;
    public static long ExpandingTimeFactor;
    public static int minLon;
    public static int maxLon;
    public static int numberOfFixes;
    public static float searchTolerance;
    public static long C2SSearchPeriod;
    public static int C2SLegs;
    public static int C2SAgents;
    public static float C2SCR;
    public static float routeAspectRatio;
    public static float continuousFactor;
    public static boolean ShowRouteResolution;
    public static boolean useDifferentialEvolution;
    public static long JIMCutoff;
    public static float waveWarning;
    public static int fontSize;

    // Runtime state (not from config)
    public static int spinnerCounter = 0;
    public static String[] spinner = {"|", "/", "-", "\\"};
    public static Shoreline shoreline;

    /**
     * Initialize all configuration from the Config singleton.
     */
    private static void initFromConfig() {
        Config cfg = Config.get();

        SCREEN = cfg.getScreen();
        ROUTE = cfg.getRoute();
        root = cfg.getRoot();
        WindSource = cfg.getWindSource();
        WindResolution = cfg.getWindResolution();
        prevailingTransitionPeriod = cfg.getPrevailingTransitionPeriod();
        sparsePolar = cfg.isSparsePolar();
        polarHighWindOnly = cfg.isPolarHighWindOnly();
        useWater = cfg.isUseWater();
        crossDateLine = cfg.isCrossDateLine();
        useIceZone = cfg.isUseIceZone();
        WVSResolution = cfg.getWvsResolution();
        ChartOffsetY = cfg.getChartOffsetY();
        ChartOffsetX = cfg.getChartOffsetX();
        REPLAY = cfg.getReplay();
        ExpandingTimeFactor = cfg.getExpandingTimeFactor();
        minLon = cfg.getMinLon();
        maxLon = cfg.getMaxLon();
        numberOfFixes = cfg.getNumberOfFixes();
        searchTolerance = cfg.getSearchTolerance();
        C2SSearchPeriod = cfg.getC2sSearchPeriod();
        C2SLegs = cfg.getC2sLegs();
        C2SAgents = cfg.getC2sAgents();
        C2SCR = cfg.getC2sCR();
        routeAspectRatio = cfg.getRouteAspectRatio();
        continuousFactor = cfg.getContinuousFactor();
        ShowRouteResolution = cfg.isShowRouteResolution();
        useDifferentialEvolution = cfg.isUseDifferentialEvolution();
        JIMCutoff = cfg.getJimCutoff();
        waveWarning = cfg.getWaveWarning();
        fontSize = cfg.getFontSize();

        // Initialize shoreline after WVSResolution is set
        shoreline = new Shoreline(WVSResolution);
    }

    public static void main(String[] args) {
        // Load configuration
        String configFile = "config.yaml";
        if (args.length > 0) {
            configFile = args[0];
        }

        try {
            Config.load(configFile);
            initFromConfig();
            logger.info("Configuration loaded from: {}", configFile);
        } catch (FileNotFoundException e) {
            logger.error("Configuration file not found: {}", configFile);
            logger.error("Please create a config.yaml file or specify a config file as argument.");
            logger.error("Usage: java -jar jim.jar [config.yaml]");
            System.exit(1);
        }

        Fix.InitSpares();
        StreamFrame streamFrame = new StreamFrame();
        streamFrame.setVisible(true);
    }
}
