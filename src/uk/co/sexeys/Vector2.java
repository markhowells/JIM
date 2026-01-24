package uk.co.sexeys;

/**
 * Created by Jim on 17/09/2017.
 *
 */
public class Vector2 {
    public float x;
    public float y;

    public Vector2() {}

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2(double x, double y) {
        this.x = (float) x;
        this.y = (float) y;
    }

    public Vector2(Vector2 a) {
        x = a.x;
        y = a.y;
    }

    public Vector2(double degrees) {
        double r = Math.toRadians(degrees);
        x = (float) Math.sin(r);
        y = (float) Math.cos(r);
    }

    void copy(Vector2 a) {
        x = a.x;
        y = a.y;
    }
    void zero () {
        x = 0;
        y = 0;
    }

    public float mag2() {
        return (x * x + y * y);
    }
    public float mag2(float c) {
        return (x * x * c * c + y * y);
    }

    public float mag() {
        return (float) Math.sqrt(this.mag2());
    }
//    float mag(float cosLatitude) {
//        return (float) Math.sqrt(x * x *cosLatitude * cosLatitude + y * y);
//    }

//    float dotN(Vector2 a) {
//        return (x * a.x + y * a.y) / mag() / a.mag();
//    }

    public float dot(Vector2 a) {
        return (x * a.x + y * a.y);
    }

//    float dot (Vector2 a,float cosLat) {
//        return (x * a.x *cosLat*cosLat + y * a.y);
//    }

//    float dotR(Vector2 a) {
//        return (x * a.y - y * a.x) / a.mag() / a.mag();
//    } // ??????? correct magnitude

    public Vector2 minus(Vector2 b) {
        return new Vector2(x - b.x, y - b.y);
    }

//    Vector2 minus(float X, float Y) {
//        return new Vector2(x - X, y - Y);
//    }

    public void minus(Vector2 b, Vector2 result) {
        result.x = x - b.x;
        result.y = y - b.y;
    }

    public Vector2 plus(Vector2 b) {
        return new Vector2(x + b.x, y + b.y);
    }



    public void scaleIP(float b) {
        x *= b;
        y *= b;
    }

    public Vector2 scale(double b) {
        return new Vector2(x * b, y * b);
    }

    public void negate() {
        x *=-1;
        y *=-1;
    }
//    void rotate(float degrees) {
//        double r = Math.toRadians(degrees);
//        double c = Math.cos(r);
//        double s = Math.sin(r);
//        double xd = c*x - s*y;
//        double yd = s*x + c*y;
//        x = (float) xd;
//        y = (float) yd;
//    }


    public float  normalise() {
        float divisor = mag();
        if (divisor != 0) {
            x /= divisor;
            y /= divisor;
        } else {
            x = 0;
            y = 0;
        }
        return divisor;
    }
//    void normalise(float cosLatitude) {
//        float divisor = mag(cosLatitude);
//        if (divisor != 0) {
//            x = x *cosLatitude / divisor;
//            y /= divisor;
//        } else {
//            x = 0;
//            y = 0;
//        }
//    }

    Vector2 scaleAdd(float s, Vector2 b) {
        return new Vector2(x + s * b.x, y + s * b.y);
    }

//    Vector2 scaleAddR(float s, Vector2 b) {
//        return new Vector2(x + s * b.y, y - s * b.x);
//    }

    static Vector2 diff(Vector2 a, Vector2 b) {
        return new Vector2(a.x - b.x, a.y - b.y);
    }


    static float area(Vector2[] p, int[] index) {
        float area = 0;
        int len = index.length;
        Vector2 p1 = p[index[0]], p2 = p[index[index.length - 1]];
        area += (p2.x + p1.x) * (p2.y - p1.y);
        int i = 1;
        while (i < len) {
            p2 = p1;
            p1 = p[index[i]];
            area += (p2.x + p1.x) * (p2.y - p1.y);
            i++;
        }
        return area / 2;
    }

    static float area(Vector2[] p) {
        float area = 0;
        int len = p.length;
        Vector2 p1 = p[0], p2 = p[len - 1];
        area += (p2.x + p1.x) * (p2.y - p1.y);
        int i = 1;
        while (i < len) {
            p2 = p1;
            p1 = p[i];
            area += (p2.x + p1.x) * (p2.y - p1.y);
            i++;
        }
        return area / 2;
    }

//    static float secondMomentY(Vector2[] p, int[] index) {
//        float moment = 0;
//        int len = index.length;
//        Vector2 p1 = p[index[0]], p2 = p[index[index.length - 1]];
//        moment += (p1.y * p1.y + p1.y * p2.y + p2.y * p2.y) * (p1.x * p2.y - p2.x * p1.y);
//        int i = 1;
//        while (i < len) {
//            p2 = p1;
//            p1 = p[index[i]];
//            moment += (p1.y * p1.y + p1.y * p2.y + p2.y * p2.y) * (p1.x * p2.y - p2.x * p1.y);
//            i++;
//        }
//        return Math.abs(moment / 12);
//    }

//    static float secondMomentX(Vector2[] p, int[] index) {
//        float moment = 0;
//        int len = index.length;
//        Vector2 p1 = p[index[0]], p2 = p[index[index.length - 1]];
//        moment += (p1.x * p1.x + p1.x * p2.x + p2.x * p2.x) * (p1.x * p2.y - p2.x * p1.y);
//        int i = 1;
//        while (i < len) {
//            p2 = p1;
//            p1 = p[index[i]];
//            moment += (p1.x * p1.x + p1.x * p2.x + p2.x * p2.x) * (p1.x * p2.y - p2.x * p1.y);
//            i++;
//        }
//        return Math.abs(moment / 12);
//    }

    static float momentY(Vector2[] p, int[] index) {
        float moment = 0;
        int len = index.length;
        Vector2 p1 = p[index[0]], p2 = p[index[index.length - 1]];
        moment += (p1.y + p2.y) * (p1.x * p2.y - p2.x * p1.y);
        int i = 1;
        while (i < len) {
            p2 = p1;
            p1 = p[index[i]];
            moment += (p1.y + p2.y) * (p1.x * p2.y - p2.x * p1.y);
            i++;
        }
        return moment / 6;
    }

    static float momentY(Vector2[] p) {
        float moment = 0;
        int len = p.length;
        Vector2 p1 = p[0], p2 = p[len - 1];
        moment += (p1.y + p2.y) * (p1.x * p2.y - p2.x * p1.y);
        int i = 1;
        while (i < len) {
            p2 = p1;
            p1 = p[i];
            moment += (p1.y + p2.y) * (p1.x * p2.y - p2.x * p1.y);
            i++;
        }
        return moment / 6;
    }

    static float momentX(Vector2[] p, int[] index) {
        float moment = 0;
        int len = index.length;
        Vector2 p1 = p[index[0]], p2 = p[index[index.length - 1]];
        moment += (p1.x + p2.x) * (p1.x * p2.y - p2.x * p1.y);
        int i = 1;
        while (i < len) {
            p2 = p1;
            p1 = p[index[i]];
            moment += (p1.x + p2.x) * (p1.x * p2.y - p2.x * p1.y);
            i++;
        }
        return moment / 6;
    }

    static float momentX(Vector2[] p) {
        float moment = 0;
        int len = p.length;
        Vector2 p1 = p[0], p2 = p[len - 1];
        moment += (p1.x + p2.x) * (p1.x * p2.y - p2.x * p1.y);
        int i = 1;
        while (i < len) {
            p2 = p1;
            p1 = p[i];
            moment += (p1.x + p2.x) * (p1.x * p2.y - p2.x * p1.y);
            i++;
        }
        return moment / 6;
    }

    static float sumY(Vector2[] p, int[] index) {
        float s = 0;
        int len = index.length;
        int i = 0;
        while (i < len) {
            s += p[index[i]].y;
            i++;
        }
        return s;
    }

    static float sumLength(Vector2[] p) {
        Vector2 p1 = p[0], p2 = p[1];
        float s = 0;
        int len = p.length - 1;
        int i = 2;
        while (i < len) {
            s += Vector2.diff(p1, p2).mag();
            p1 = p2;
            p2 = p[i];
            i++;
        }
        return s;
    }

    static float moment(Vector2[] p, int[] index, float[] d) {
        float area = 0;
        int len = index.length;
        Vector2 p1 = p[index[0]], p2 = p[index[index.length - 1]];
        float d1 = d[0], d2 = d[index.length - 1];
        area += (p2.x + p1.x) * (p2.y * d2 - p1.y * d1);
        int i = 1;
        while (i < len) {
            p2 = p1;
            d2 = d1;
            p1 = p[index[i]];
            d1 = d[i];
            area += (p2.x + p1.x) * (p2.y * d2 - p1.y * d1);
            i++;
        }
        return Math.abs(area / 2);
    }


    static double length(Vector2[] p, int[] index) {
        Vector2 p1 = p[index[0]];
        Vector2 p2 = p[index[1]];
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        double l = Math.sqrt(dx * dx + dy * dy);
        if (l == 0)
            l = 1e-14; // stops divide by zero downstream
        return l;
    }

    static double distance(Vector2 p1, Vector2 p2, Vector2 p) {
        double y2y1 = p2.y - p1.y;
        double x2x1 = p2.x - p1.x;
        return Math.abs(y2y1 * p.x - x2x1 * p.y + p2.x * p1.y - p2.y * p1.x) /
                Math.sqrt(y2y1 * y2y1 + x2x1 * x2x1);
    }

    static double distance(Vector2[] p, int[] line, int point) {
        Vector2 p1 = p[line[0]];
        Vector2 p2 = p[line[1]];
        Vector2 p0 = p[point];
        return distance(p1, p2, p0);
    }

    static boolean intersection(Vector2[] p, int[] index, Vector2 i) {
        Vector2 p0 = p[index[0]];
        Vector2 p1 = p[index[1]];
        Vector2 p2 = p[index[2]];
        Vector2 p3 = p[index[3]];
        float s1_x, s1_y, s2_x, s2_y;
        s1_x = p1.x - p0.x;
        s1_y = p1.y - p0.y;
        s2_x = p3.x - p2.x;
        s2_y = p3.y - p2.y;

        float s, t;
        s = (-s1_y * (p0.x - p2.x) + s1_x * (p0.y - p2.y)) / (-s2_x * s1_y + s1_x * s2_y);
        t = (s2_x * (p0.y - p2.y) - s2_y * (p0.x - p2.x)) / (-s2_x * s1_y + s1_x * s2_y);

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            // Collision detected
            i.x = p0.x + (t * s1_x);
            i.y = p0.y + (t * s1_y);
            return true;
        }
        i.x = 0;
        i.y = 0;
        return false; // No collision
    }

    static boolean findIntersection(Vector2[] p, int[] index, Vector2 intersection) {
        int[] subIndex = new int[4];
        subIndex[0] = index[0];
        subIndex[1] = index[1];
        for (int i = 2; i < index.length - 1; i++) {
            subIndex[2] = index[i];
            subIndex[3] = index[i + 1];
            if (intersection(p, subIndex, intersection))
                return true;
        }
        return false;
    }


    static double maxDistance(Vector2[] p, int[] line, int[] point) {
        double max = Double.MIN_VALUE;
        for (int pt : point) {
            double d = distance(p, line, pt);
            if (d > max)
                max = d;
        }
        return max;
    }

    static Vector2 interpolate(Vector2[] p, int[] line, float t) {
        Vector2 p0 = p[line[0]];
        Vector2 p1 = p[line[1]];
        Vector2 result = new Vector2(0, 0);
        result.x = p0.x + (p1.x - p0.x) * t;
        result.y = p0.y + (p1.y - p0.y) * t;
        return result;
    }

    static Vector2 interpolate(Vector2[] p, float t) {
        int i = (int) Math.floor(t);
        float f = t - i;
        Vector2 p0 = p[i];
        Vector2 p1 = p[i + i];
        Vector2 result = new Vector2(0, 0);
        result.x = p0.x + (p1.x - p0.x) * f;
        result.y = p0.y + (p1.y - p0.y) * f;
        return result;
    }

    static Vector2 interpolate(Vector2[] p, double t) {
        return interpolate(p, (float) t);
    }

    static Vector2 closestPoint(Vector2[] p, int[] line, int point) {
        Vector2 p1 = p[line[0]];
        Vector2 p2 = p[line[1]];
        Vector2 p0 = p[point];
        Vector2 n = p2.minus(p1);
        n.normalise();
        return p1.scaleAdd(-1 * (p1.minus(p0).dot(n)), n);
    }

// TODO don't now how this ever worked. need 4 vectors to do this calculation
    static Vector2 closestPoint(Vector2 p1, Vector2 p2, Vector2 p0) {
        Vector2 n = p2.minus(p1);
        n.normalise();
        return p1.scaleAdd(-1 * (p1.minus(p0).dot(n)), n);
    }

    public Vector2 toBearing() {
        return new Vector2(Math.sqrt(x * x + y * y), Math.atan2(x, y) * 180 / Math.PI);
    }

    Vector2 Perpendicular() {
        return new Vector2(-y,+x);
    }

    public static int orientation(Vector2 p, Vector2 q, Vector2 r)
    {
        // See https://www.geeksforgeeks.org/orientation-3-ordered-points/
        // for details of below formula.;
        float val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);

        if (val == 0) return 0;  // colinear

        return (val > 0)? -1: 1; // clock or counterclock wise
    }
}

