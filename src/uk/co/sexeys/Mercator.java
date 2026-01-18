package uk.co.sexeys;


/**
 * Created by Jim on 15/06/2018.
 *
 */
public class Mercator {
    public float lat1;
    public float lon1;
    public float lat2;
    public float lon2;
    int[] x1, y1;
    public int[] x2;
    public int[] y2;
    Vector2 _pixelOrigin;
    public Vector2 topLeft, bottomRight;
    public Boolean enabled = false;
    double _pixelsPerLonDegree;
    double _pixelsPerLonRadian;
    public int height, width;
    public float scale;

    public Mercator() {}

    Mercator(int[] x1, int[] y1, int x2[], int y2[], float lat1, float lon1, float lat2, float lon2) {
        this.x1 = x1;
        this.y1 = y1;
        this.lat1 = lat1;
        this.lon1 = lon1;
        this.x2 = x2;
        this.y2 = y2;
        this.lat2 = lat2;
        this.lon2 = lon2;
        this.scale = Math.abs(lat2 - lat1) * 60 * (float) phys.mPerNM;

    }
    Mercator(int x1, int y1, double lat1, double lon1, int x2, int y2, double lat2, double lon2) {
        this.x1 = new int[]{x1};
        this.y1 = new int[]{y1};
        this.lat1 = (float) lat1;
        this.lon1 = (float) lon1;
        this.x2 = new int[]{x2};
        this.y2 = new int[]{y2};
        this.lat2 = (float) lat2;
        this.lon2 = (float) lon2;
        this.scale = (float) (Math.abs(lat2 - lat1) * 60 * phys.mPerNM);
    }

    Mercator(int x1, int y1, float lat1, float lon1, int x2, int y2, float lat2, float lon2) {
        this.x1 = new int[]{x1};
        this.y1 = new int[]{y1};
        this.lat1 = lat1;
        this.lon1 = lon1;
        this.x2 = new int[]{x2};
        this.y2 = new int[]{y2};
        this.lat2 = lat2;
        this.lon2 = lon2;
        this.scale = (float) (Math.abs(lat2 - lat1) * 60 * phys.mPerNM);
    }

    public Mercator(float top, float left, float right) { // relies on screen resize to update
        this.x1 = new int[]{0};
        this.y1 = new int[]{0};
        this.x2 = new int[]{0};
        this.y2 = new int[]{0};
        this.lat1 =  top; this.lon1 = left;
        this.lon2 = right;
        _pixelOrigin = new Vector2(0, 0); // VERY DODGY only here for debug purposes
        this.scale = (float) (Math.abs(lat2 - lat1) * 60 * phys.mPerNM);
    }

    Mercator(double lat1, double lon1, double lat2, double lon2) {
        _pixelsPerLonDegree = 1 ;
        _pixelsPerLonRadian = -_pixelsPerLonDegree * 360 / (2 * Math.PI);
        _pixelOrigin = new Vector2(0, 0);
        _pixelOrigin = fromLatLngToPoint(lat1, lon1);
        _pixelOrigin.x *= -1;
        _pixelOrigin.y *= -1;
        Vector2 t = fromPointToLatLng(new Vector2(0, 1));
        _pixelsPerLonDegree = phys.mPerNM *60* (  t.y - lat1 );
        _pixelsPerLonRadian = -_pixelsPerLonDegree * 360 / (2 * Math.PI);
        _pixelOrigin = new Vector2(0, 0);
        _pixelOrigin = fromLatLngToPoint(lat1, lon1);
        _pixelOrigin.x *= -1;
        _pixelOrigin.y *= -1;
        this.x1 = new int[]{0};
        this.y1 = new int[]{0};
        this.x2 = new int[]{(int)Math.round(_pixelsPerLonDegree*(lon2 - lon1))};
        this.y2 = new int[]{(int)Math.round(phys.mPerNM *60*(lat2-lat1))};
        this.scale = (float) ( Math.abs(lat2 - lat1) * 60 * phys.mPerNM);

    }
    public Mercator(Vector2 position, double  scale) {
        _pixelsPerLonDegree = scale;
        _pixelsPerLonRadian = _pixelsPerLonDegree * 360 / (2 * Math.PI);
        _pixelOrigin = new Vector2(0, 0);
        _pixelOrigin = fromLatLngToPoint(position.y*phys.degrees, position.x*phys.degrees);
        _pixelOrigin.x *= -1;
        _pixelOrigin.y *= -1;
    }



    public void computeParameters(int i) {
        _pixelsPerLonDegree = (x1[i] - x2[i]) / (lon1 - lon2);
        _pixelsPerLonRadian = _pixelsPerLonDegree * 360 / (2 * Math.PI);
        _pixelOrigin = new Vector2(-x1[i], -y1[i]);
        _pixelOrigin = fromLatLngToPoint(lat1, lon1);
        _pixelOrigin.x *= -1;
        _pixelOrigin.y *= -1;
        topLeft = fromPointToLatLng(new Vector2(x1[i], y1[i]));
        bottomRight = fromPointToLatLng(new Vector2(x2[i], y2[i]));
        scale = (float) ( Math.abs(lat2 - lat1) * 60 * phys.mPerNM);
    }

    Vector2 fromLatLngToPoint(Vector2 p) {
        return fromLatLngToPoint(p.y, p.x);
    }

    public Vector2 fromRadiansToPoint(Vector2 p) {
        return fromLatLngToPoint(p.y*phys.degrees, p.x*phys.degrees);
    }

    public Vector2 fromLatLngToPoint(double lat, double lng) {
        Vector2 point = new Vector2(0, 0);

        point.x = (float)(_pixelOrigin.x + lng * _pixelsPerLonDegree);

        // Truncating to 0.9999 effectively limits latitude to 89.189. This is
        double siny = bound(MathUtil.sind(lat), -0.9999, 0.9999);
        point.y = (float)(_pixelOrigin.y - _pixelsPerLonRadian * Math.log((1 + siny) / (1 - siny)) / 2);

        return point;
    }

    public double fromLengthToPixels(Vector2 position, float length) {
        double siny = bound(Math.sin(position.y), -0.9999, 0.9999);
        double siny1 = bound(Math.sin(position.y+length/phys.R), -0.9999, 0.9999);

        double res = _pixelsPerLonRadian * ( Math.log((1 + siny) / (1 - siny)) / 2 - Math.log((1 + siny1) / (1 - siny1)) / 2);
        return Math.abs(res);
    }

    Vector2 fromPointToLatLng(Vector2 point) {
        return fromPointToLatLng(point.x, point.y);
    }

    public Vector2 fromPointToLatLng(double x, double y) {
        double lng = (x - _pixelOrigin.x) / _pixelsPerLonDegree;
        double latRadians = (y - _pixelOrigin.y) / -_pixelsPerLonRadian;
        double lat = (2 * Math.atan(Math.exp(latRadians)) - Math.PI / 2) * phys.degrees;
        return new Vector2(lng, lat);
    }

    Boolean contains(Vector2 p) {
        if (bottomRight.x < p.x)
            return false;
        if (bottomRight.y > p.y)
            return false;
        if (topLeft.x > p.x)
            return false;
        if (topLeft.y < p.y)
            return false;
        return true;
    }

    Boolean contains(Mercator c) {
        if (bottomRight.x < c.bottomRight.x)
            return false;
        if (bottomRight.y > c.bottomRight.y)
            return false;
        if (topLeft.x > c.topLeft.x)
            return false;
        if (topLeft.y < c.topLeft.y)
            return false;
        return true;
    }

    void toggleVisibility() {
        if (enabled)
            disable();
        else
            enable();
    }

    private double bound(double val, double valMin, double valMax) {
        double res;
        res = Math.max(val, valMin);
        res = Math.min(res, valMax);
        return res;
    }

    void enable() {
        enabled = true;
    }

    void disable() {
        enabled = false;
    }


    static float buildAngle(int d, float m, String s) {
        float f;
        if(s.contains("N")  ||  s.contains("E"))
            f = d + m/60f;
        else
            f = -d -m/60f;
        return f;
    }

    //https://www.movable-type.co.uk/scripts/latlong.html
}
