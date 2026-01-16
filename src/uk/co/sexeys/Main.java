package uk.co.sexeys;

public class Main {
    static final public String SCREEN = "53*00'N 11*30'W 37*00'N 0*0'W";
    static final public String ROUTE = """
Using Polar:ELEMENTAL
Using Wind: GFS20251124180601678.grb
Using Wind: GFS20251124180601679.grb
Using Tide: tide20251124.nc
Using Waves: GFS20251124180601680.grb
Depart: 50*34'39"N 002*24'26"W 2025/11/25 15:00 UTC
Expand: 20 nm 360 bins 0.1 hour step
Obstruction: 49*46'15"N 002*50'15"W;50*03'31"N 002*57'22"W;50*08'34"N 002*28'14"W;49*51'17"N 002*21'03"W;49*46'15"N 002*50'15"W Mid Chennel
Destination: 49*40'24"N 001*39'24"W 0.2 nm 360 bins of 1 nm 0.1 hour step
""";
    static final public String root = ".\\database\\";
    static final public String WindSource = "https://nomads.ncep.noaa.gov/pub/data/nccf/com/gfs/prod/"; // or "https://ftp.ncep.noaa.gov/data/nccf/com/gfs/prod/"; or "https://ftpprd.ncep.noaa.gov/data/nccf/com/gfs/prod/"
//    static final public String WindSource =  "https://ftp.ncep.noaa.gov/data/nccf/com/gfs/prod/"; // "https://ftpprd.ncep.noaa.gov/data/nccf/com/gfs/prod/"
    static final public String WindResolution = "1p00"; // 1p00, 0p50 or 0p25, 1p00. Not for Virtual Regatta
    static final public long prevailingTransitionPeriod = 15 * phys.msPerDay;
    static final public boolean sparsePolar = false;
    static final public boolean polarHighWindOnly = false; // Attempt to model in ability to sail in low wind speeds due to sails flapping.

    static final public boolean useWater = true;
    static final public boolean crossDateLine = false;
    static final public boolean useIceZone = false;
    static final public int WVSResolution  =  100000; // 250000 = slow
    static final float ChartOffsetY = 0/3600f; // +ve moves chart up
    static final float ChartOffsetX = -0/3600f; // +ve moves chart right
    static final public String REPLAY = "";
    public static final long ExpandingTimeFactor = 30; // 1/30 is kind of 2/60 = 2 degrees to ensure crossover
    public static int minLon = -20; // TODO Limits the size of the shoreline database. Needs to change...
    public static int maxLon = 0;
    static final public int numberOfFixes = 80;  // number of measurement points used in C2S bigger = slower
    static final public float searchTolerance = 100; // meters (10 for VR). How close C2S needs to get to each waypoint smaller = slower
    static final long C2SSearchPeriod =5 * phys.msPerHour; // How far along JIM that C2S places its initial destination
    static final int C2SLegs = 8; // Number of legs in C2S solution
    static final int C2SAgents = 10 * C2SLegs; // https://en.wikipedia.org/wiki/Differential_evolution
    static final float C2SCR = 0.9f ; // Cross over probability - see wiki page.
    static final float routeAspectRatio = 1.0f;
    static public float continuousFactor = 0.3f;  // factor for continuous search in DE (obsolete?)
    static public final boolean ShowRouteResolution = false  ; // false for maps that scales with screen resolution
    static public final boolean useDifferentialEvolution = false;
    public static int spinnerCounter = 0;
    public static String[] spinner = {"|","/","-","\\"};
    static public final Shoreline shoreline = new Shoreline(WVSResolution);

    public static final long JIMCutoff = (long)(100 * phys.msPerHour);
    public static final float waveWarning = 3; //m
    public static final int fontSize = 30;

    public static void main(String[] args) {
        Fix.InitSpares();
        StreamFrame streamFrame = new StreamFrame();
        streamFrame.setVisible(true);
    }
}
