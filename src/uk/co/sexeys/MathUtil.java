package uk.co.sexeys;

/**
 * Methods for doing mathematical operations with doubles.
 *
 * @author Brent Bryan
 */
class MathUtil {
    private MathUtil() {
    }

    public static final double TWO_PI = 2 * Math.PI;
    static final double DEGREES_TO_RADIANS = Math.PI / 180;
    static final double RADIANS_TO_DEGREES = 180 / Math.PI;

    /**
     * Returns x if x <= y, or x-y if not. While this utility performs a role similar to a modulo
     * operation, it assumes x >=0 and that x < 2y.
     */
    public static double quickModulo(double x, double y) {
        if (x > y) return x - y;
        return x;
    }

    /**
     * Returns a random number between 0 and f.
     */
    public static double random(double f) {
        return (Math.random()) * f;
    }

    static final double arcsecToRad = Math.PI / 648000;
    public static final double arcsecToDeg = arcsecToRad * RADIANS_TO_DEGREES;

    //Sine of angles in degrees
    static double sind(double x) {
        return Math.sin(DEGREES_TO_RADIANS * x);
    }

    //Cosine of angles in degrees
    static double cosd(double x) {
        return Math.cos(DEGREES_TO_RADIANS * x);
    }

    //Tangent of angles in degrees
    public static double tand(double x) {
        return (double) Math.tan(DEGREES_TO_RADIANS * x);
    }

}