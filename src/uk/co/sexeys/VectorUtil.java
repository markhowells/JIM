package uk.co.sexeys;

class VectorUtil {
    static Vector3 zero() {
        return new Vector3(0, 0, 0);
    }

    static double dotProduct(Vector3 p1, Vector3 p2) {
        return p1.x * p2.x + p1.y * p2.y + p1.z * p2.z;
    }

    static Vector3 crossProduct(Vector3 p1, Vector3 p2) {
        return new Vector3(p1.y * p2.z - p1.z * p2.y,
                -p1.x * p2.z + p1.z * p2.x,
                p1.x * p2.y - p1.y * p2.x);
    }

    public static double angleBetween(Vector3 p1, Vector3 p2) {
        return Math.acos(dotProduct(p1, p2) / (length(p1) * length(p2)));
    }

    public static double length(Vector3 v) {
        return Math.sqrt(lengthSqr(v));
    }

    static double lengthSqr(Vector3 v) {
        return dotProduct(v, v);
    }

    static Vector3 normalized(Vector3 v) {
        double len = length(v);
        if (len < 0.000001f) {
            return zero();
        }
        return scale(v, 1.0f / len);
    }

    public static Vector3 project(Vector3 v, Vector3 onto) {
        return scale(dotProduct(v, onto) / length(onto), onto);
    }

    static Vector3 projectOntoUnit(Vector3 v, Vector3 onto) {
        return scale(dotProduct(v, onto), onto);
    }

    public static Vector3 projectOntoPlane(Vector3 v, Vector3 unitNormal) {
        return difference(v, projectOntoUnit(v, unitNormal));
    }

    static Vector3 negate(Vector3 v) {
        return new Vector3(-v.x, -v.y, -v.z);
    }

    static Vector3 sum(Vector3 v1, Vector3 v2) {
        return new Vector3(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
    }

    static Vector3 difference(Vector3 v1, Vector3 v2) {
        return sum(v1, negate(v2));
    }

    static Vector3 scale(double factor, Vector3 v) {
        return new Vector3(v.x * factor, v.y * factor, v.z * factor);
    }

    static Vector3 scale(Vector3 v, double factor) {
        return scale(factor, v);
    }

    static Vector3 unit(double angle) {
        return new Vector3(MathUtil.cosd(angle),MathUtil.sind(angle),0);
    }

    static Vector3 rotateZ(Vector3 v,double angle) {
        double cosA = MathUtil.cosd(angle);
        double sinA = MathUtil.sind(angle);
        return new Vector3(v.x * cosA - v.y * sinA, v.x * sinA + v.y * cosA, v.z);
    }

    static Vector3 rotateY(Vector3 v,double angle) {
        double cosA = MathUtil.cosd(angle);
        double sinA = MathUtil.sind(angle);
        return new Vector3(v.x * cosA + v.z * sinA, v.y, -v.x * sinA + v.z * cosA);
    }
}
