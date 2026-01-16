package uk.co.sexeys;

public class Vector3 {

    public double x;
    public double y;
    public double z;

    public Vector3() {}

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3(double[] xyz) throws IllegalArgumentException {
        if (xyz.length != 3) {
            throw new IllegalArgumentException("Trying to create 3 vector from array of length: " + xyz.length);
        }
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
    }

    Vector3 copy() {
        return new Vector3(x, y, z);
    }

    /**
     * Assigns these values to the vector's components.
     */
    public void assign(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Assigns the values of the other vector to this one.
     */
    public void assign(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    /**
     * Normalize the vector in place, i.e., map it to the corresponding unit vector.
     */
    public void normalize() {
        double norm = this.length();
        this.x = this.x / norm;
        this.y = this.y / norm;
        this.z = this.z / norm;
    }

    /**
     * Scale the vector in place.
     */
    public void scale(double scale) {
        this.x = this.x * scale;
        this.y = this.y * scale;
        this.z = this.z * scale;
    }

    void add(Vector3 v1) {
        x += v1.x;
        y += v1.y;
        z += v1.z;
    }

    public double[] toArray() {
        return new double[]{x, y, z};
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Vector3)) return false;
        Vector3 other = (Vector3) object;
        // double equals is a bit of a dodgy concept
        return other.x == x && other.y == y && other.z == z;
    }

    @Override
    public int hashCode() {
        int result = 17;
        long f = Double.doubleToLongBits(x);
        result = 37 * result + (int) (f ^ (f >>> 32));
        f = Double.doubleToLongBits(y);
        result = 37 * result + (int) (f ^ (f >>> 32));
        f = Double.doubleToLongBits(z);
        result = 37 * result + (int) (f ^ (f >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("x=%.1f, y=%.1f, z=%.1f", x, y, z);
    }

    double dot(Vector3 p1) {
        return p1.x * x + p1.y * y + p1.z * z;
    }
}

// https://stackoverflow.com/questions/113511/best-implementation-for-hashcode-method