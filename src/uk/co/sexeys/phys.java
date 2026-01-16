package uk.co.sexeys;

/**
 * Created by Jim on 09/09/2017.
 *
 */
public class phys {
//    % this file contains physical parameters for the vpp

    //    %%%%%% WATER %%%%%%%
    static final double rho_w   =1025.9;
    static final double ni_w    =1.18838E-6;
    //            %%%%%% AIR %%%%%%%
    static final double rho_a   =1.125;
    //            %%%%%% GENERAL %%%
    static final double g       =9.80665;
    // all speeds in m/s
    static final public float knots   =1/0.514444f;
    // All distances in m
    static final public double R = 6371e3;  // Earths diameter metres
    public static final float rReciprocal = 1/(float) R;
    static final public double mPerNM = 1852;
    static final double NM = 1/ mPerNM;
    // Akk angles in radians
    static final public float degrees = 180.f/ (float) Math.PI;
    static final public double radiansPerDegree = 1/ degrees;

    // All times are in milliseconds
    public static final long msPerSecond = 1000;
    public static final long msPerMinute = 60* msPerSecond;
    static final public long msPerHour = 60* msPerMinute;
    public static final long msPerDay = 24* msPerHour;
}