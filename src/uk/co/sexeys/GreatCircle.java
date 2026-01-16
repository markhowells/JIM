package uk.co.sexeys;

import uk.co.sexeys.waypoint.Diode;

// https://en.wikipedia.org/wiki/Great-circle_navigation
// https://www.movable-type.co.uk/scripts/latlong.html
// https://dtcenter.org/sites/default/files/community-code/met/docs/write-ups/gc_simple.pdf

public class GreatCircle {
    double phi1, lamda1; // latitude and longitude of start
    double phi2, lamda2; // latitude and longitude of end

    double alpha0; // the heading of the great circle at the equator
    public double alpha1; // the heading of the great circle at the start
    public double alpha2; // the heading of the great circle at the end
    double Salpha0, Calpha0; // sin and cos of alpha0
    double lamda0; // the longitude of great circle at the equator
    double lamda12 = lamda2 - lamda1;
    double Sphi1,Cphi1,Sphi2,Cphi2,Slamda12,Clamda12;
    public double d01;
    public double d12;

    public GreatCircle(Vector2 start, Vector2 end) {
        phi1 = start.y;
        lamda1 = start.x;
        phi2 = end.y;
        lamda2 = end.x;

        lamda12 = lamda2 - lamda1;
        Sphi1 = Math.sin(phi1);
        Cphi1 = Math.cos(phi1);
        Sphi2 = Math.sin(phi2);
        Cphi2 = Math.cos(phi2);
        Slamda12 = Math.sin(lamda12);
        Clamda12 = Math.cos(lamda12);

        alpha1 = Math.atan2(Cphi2 * Slamda12, Cphi1 * Sphi2 - Sphi1 * Cphi2 * Clamda12);
        alpha2 = Math.atan2(Cphi1 * Slamda12, -Cphi2 * Sphi1 + Sphi2 * Cphi1 * Clamda12);

        double Calpha1 = Math.cos(alpha1);
        double Salpha1 = Math.sin(alpha1);

        alpha0 = Math.atan2(
                Salpha1 * Cphi1,
                Math.sqrt(Calpha1 * Calpha1 + Salpha1 * Salpha1 * Sphi1 * Sphi1));
        Salpha0 = Math.sin(alpha0);
        Calpha0 = Math.cos(alpha0);
        d01 = Math.atan2(Math.tan(phi1), Calpha1);
        double t1 = Cphi1 * Sphi2 - Sphi1 * Cphi2 * Clamda12;
        double t2 = Cphi2 * Slamda12;
        double t3 = Sphi1 * Sphi2 + Cphi1 * Cphi2 * Clamda12;
        d12 = Math.atan2(Math.sqrt(t1 * t1 + t2 * t2), t3);
        double lamda01 = Math.atan2(
                Salpha0 * Math.sin(d01), Math.cos(d01));
        lamda0 = lamda1 - lamda01;
    }

    public Vector2 point(double d) { // d is ANGULAR distance from equator
        Vector2 P = new Vector2();
        double Sd = Math.sin(d);
        double Cd = Math.cos(d);
        P.y = (float) Math.atan2(Calpha0 * Sd, Math.sqrt(Cd * Cd + Salpha0 * Salpha0 * Sd * Sd));
        P.x = (float) (Math.atan2(Salpha0 * Sd, Cd) + lamda0);
        return P;
    }

    static public Vector2 destination(Vector2 p, double d, double b) { // d is ANGULAR distance from point p at a bearing of b
        double phi1 = p.y;
        double lamda1 = p.x;
        double Sphi1 = Math.sin(phi1);
        double Cphi1 = Math.cos(phi1);
        double Sd = Math.sin(d);

        Vector2 P = new Vector2();

        double phi2 = (Math.asin(Sphi1 * Math.cos(d) +
                Cphi1 * Sd * Math.cos(b)));
        P.x = (float) (lamda1 + Math.atan2(Math.sin(b) * Math.sin(d) * Cphi1,
                Math.cos(d) - Sphi1 * Math.sin(phi2)));
        P.y = (float) phi2;
        return P;
    }

    public Vector2 crossTrack(double d, double X, double b) { // d is ANGULAR distance from start at cross track of X
        Vector2 P = new Vector2();
        double Sd = Math.sin(d);
        double Cd = Math.cos(d);
        P.y = (float) Math.atan2(Calpha0 * Sd, Math.sqrt(Cd * Cd + Salpha0 * Salpha0 * Sd * Sd));
        P.x = (float) (Math.atan2(Salpha0 * Sd, Cd) + lamda0);
        double alpha = Math.atan2(Math.tan(alpha0), Cd);
        return destination(P, X, alpha + b);
    }

    public static float range(Vector2 start, Vector2 end) {
        double phi1 = start.y;
        double phi2 = end.y;
        double dtheta = (end.y - start.y);
        double dlamda = (end.x - start.x);

        double a = Math.sin(dtheta / 2) * Math.sin(dtheta / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(dlamda / 2) * Math.sin(dlamda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (phys.R * c); // meters
    }

    public float BearingtoEnd(Vector2 position) {
        double lamda = (position.x - lamda2) ;

        double a = Math.atan2(
                Math.sin(lamda) * Math.cos(position.y),
                Cphi2 * Math.sin(position.y) - Sphi2 * Math.cos(position.y) * Math.cos(lamda));
        return (float) (a * phys.degrees + 360) % 360;
    }

    public void XTE(Vector2 position,Vector2 result) { // radians
        // result.x XTE from great circle
        // result.y XTE from great circle at right angles to endpoint = distance along track
        double dtheta = (position.y - phi2);
        double dlamda = (position.x - lamda2);

        double a = Math.sin(dtheta / 2) * Math.sin(dtheta / 2) +
                Cphi2 * Math.cos(position.y) *
                        Math.sin(dlamda / 2) * Math.sin(dlamda / 2);
        double d = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double b = Math.atan2(
                Math.sin(dlamda) * Math.cos(position.y),
                Cphi2 * Math.sin(position.y) - Sphi2 * Math.cos(position.y) * Math.cos(dlamda));
        result.x =  (float) Math.asin(Math.sin(d)*Math.sin(b-alpha2));


        double t1 = b-(alpha2+Math.PI/2);
        double t2 = Math.sin(t1);
        double t3 = Math.sin(d);
        result.y = (float)  Math.asin(t3*t2);
    }

    public static void rangeAndBearing(Vector2 start, Vector2 end, Vector2 result) {
        double phi1 = start.y;
        double phi2 = end.y;
        double dtheta = (end.y - start.y);
        double dlamda = (end.x - start.x);
        double CPhi1 = Math.cos(phi1);
        double CPhi2 = Math.cos(phi2);

        double a = Math.sin(dtheta / 2) * Math.sin(dtheta / 2) +
                CPhi1 * Math.cos(phi2) *
                        Math.sin(dlamda / 2) * Math.sin(dlamda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        result.y =  (float) (phys.R * c); // meters

        a = Math.atan2(
                Math.sin(dlamda) * CPhi2,
                CPhi1 * Math.sin(phi2) - Math.sin(phi1) * CPhi2 * Math.cos(dlamda));
        result.x =  (float) (a * phys.degrees + 360) % 360; // degrees
    }
}
