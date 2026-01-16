package uk.co.sexeys;

/**
 * Created by Jim on 15/07/2018.
 *
 */

public class BiLinear {
    Vector2[][] value;
    float[] xp,yp;
    private int lastXIndex = 0, lastYIndex = 0;

    BiLinear () {
    }

    BiLinear (BiLinear b) {
        value = b.value.clone();
        xp = b.xp.clone();
        yp = b.yp.clone();
        lastXIndex = b.lastXIndex;
        lastYIndex = b.lastYIndex;
    }

    BiLinear (Vector2[][] v, float[] x, float[] y) {
        value = v;
        xp = x;
        yp = y;
    }
    public void interpolate (float x, float y, Vector2 result) throws Exception {

        try {
            if(x < xp[lastXIndex])
                while (x < xp[lastXIndex])
                    lastXIndex --;
            else if(x> xp[lastXIndex+1])
                while (x > xp[lastXIndex+1])
                    lastXIndex ++;
            if(y < yp[lastYIndex])
                while (y < yp[lastYIndex])
                    lastYIndex --;
            else if(y> yp[lastYIndex+1])
                while (y > yp[lastYIndex+1])
                    lastYIndex ++;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new Exception("Array out of bounds");
        }

        float fx = (x - xp[lastXIndex])/(xp[lastXIndex+1] - xp[lastXIndex]);
        float fy = (y - yp[lastYIndex])/(yp[lastYIndex+1] - yp[lastYIndex]);

        float vx0 = fx * (value[lastXIndex+1][lastYIndex].x - value[lastXIndex][lastYIndex].x)
                + value[lastXIndex][lastYIndex].x;
        float vx1 = fx * (value[lastXIndex+1][lastYIndex+1].x - value[lastXIndex][lastYIndex+1].x)
                + value[lastXIndex][lastYIndex+1].x;
        result.x = fy * (vx1 - vx0) + vx0;

        vx0 = fx * (value[lastXIndex+1][lastYIndex].y - value[lastXIndex][lastYIndex].y)
                + value[lastXIndex][lastYIndex].y;
        vx1 = fx * (value[lastXIndex+1][lastYIndex+1].y - value[lastXIndex][lastYIndex+1].y)
                + value[lastXIndex][lastYIndex+1].y;
        result.y = fy * (vx1 - vx0) + vx0;
    }

    public Vector2 interpolate (double x, double y) throws Exception {

        try {
            if(x < xp[lastXIndex])
                while (x < xp[lastXIndex])
                    lastXIndex --;
            else if(x> xp[lastXIndex+1])
                while (x > xp[lastXIndex+1])
                    lastXIndex ++;
            if(y < yp[lastYIndex])
                while (y < yp[lastYIndex])
                    lastYIndex --;
            else if(y> yp[lastYIndex+1])
                while (y > yp[lastYIndex+1])
                    lastYIndex ++;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new Exception("Array out of bounds");
        }

        double fx = (x - xp[lastXIndex])/(xp[lastXIndex+1] - xp[lastXIndex]);
        double fy = (y - yp[lastYIndex])/(yp[lastYIndex+1] - yp[lastYIndex]);

        double vx0 = fx * (value[lastXIndex+1][lastYIndex].x - value[lastXIndex][lastYIndex].x)
                + value[lastXIndex][lastYIndex].x;
        double vx1 = fx * (value[lastXIndex+1][lastYIndex+1].x - value[lastXIndex][lastYIndex+1].x)
                + value[lastXIndex][lastYIndex+1].x;
        double xr = fy * (vx1 - vx0) + vx0;

        vx0 = fx * (value[lastXIndex+1][lastYIndex].y - value[lastXIndex][lastYIndex].y)
                + value[lastXIndex][lastYIndex].y;
        vx1 = fx * (value[lastXIndex+1][lastYIndex+1].y - value[lastXIndex][lastYIndex+1].y)
                + value[lastXIndex][lastYIndex+1].y;
        double yr = fy * (vx1 - vx0) + vx0;

        return new Vector2(xr,yr);
    }
    Vector2 vectorInterpolate (double x, double y) throws Exception {

        try {
            if(x < xp[lastXIndex])
                while (x < xp[lastXIndex])
                    lastXIndex --;
            else if(x> xp[lastXIndex+1])
                while (x > xp[lastXIndex+1])
                    lastXIndex ++;
            if(y < yp[lastYIndex])
                while (y < yp[lastYIndex])
                    lastYIndex --;
            else if(y> yp[lastYIndex+1])
                while (y > yp[lastYIndex+1])
                    lastYIndex ++;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new Exception("Array out of bounds");
        }

        double fx = (x - xp[lastXIndex])/(xp[lastXIndex+1] - xp[lastXIndex]);
        double fy = (y - yp[lastYIndex])/(yp[lastYIndex+1] - yp[lastYIndex]);

        double vx0 = fx * (value[lastXIndex+1][lastYIndex].x - value[lastXIndex][lastYIndex].x)
                + value[lastXIndex][lastYIndex].x;
        double vx1 = fx * (value[lastXIndex+1][lastYIndex+1].x - value[lastXIndex][lastYIndex+1].x)
                + value[lastXIndex][lastYIndex+1].x;
        double xr = fy * (vx1 - vx0) + vx0;

        double vy0 = fx * (value[lastXIndex+1][lastYIndex].y - value[lastXIndex][lastYIndex].y)
                + value[lastXIndex][lastYIndex].y;
        double vy1 = fx * (value[lastXIndex+1][lastYIndex+1].y - value[lastXIndex][lastYIndex+1].y)
                + value[lastXIndex][lastYIndex+1].y;
        double yr = fy * (vy1 - vy0) + vy0;

//        double vw0 = vx0*vx0 + vy0*vy0;
//        double vw1 = vx1*vx1 + vy1*vy1;
//        double wr = fy * (vw1 - vw0) + vw0;
//        final double denominator = (xr * xr+  yr * yr);
//        float  factor = 0;
//        if( denominator != 0) {
//            factor = (float) Math.pow(wr/denominator,0.5);
//        }
//        return new Vector2(xr*factor,yr*factor);
        double vw0 = Math.sqrt(vx0*vx0 + vy0*vy0);
        double vw1 = Math.sqrt(vx1*vx1 + vy1*vy1);
        double wr = fy * (vw1 - vw0) + vw0;
        double s = Math.sqrt(1 - y*y);
        return new Vector2(wr*y,wr*s);
    }
}
