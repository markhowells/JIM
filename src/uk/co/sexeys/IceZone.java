package uk.co.sexeys;

import java.awt.*;

public class IceZone {
    float longitudes[] = {    0,         5,        10,        15,        20,      25,      30,      35,      40,      45,      50,       55,       60,        65,       70,       75,       80,       85,   90,   95,    100,  105,  110,  115,  120,     125,   130,     135,   140,   145,   150,      155,      160,      165,      170,      175,      180,      185,    190,      195,      200,     205,      210,   215,  220,   225,      230,      235,    240,     245,   250,   255,    260,    265,     270,    275,  280,    285,  290,     295,    300, 305,   310,   315, 320, 325,  330,     335,  340,  345,  350,     355};
    double latitudes[] = {-44.0,  -44.2500,  -44.8166,  -45.4166,  -46.0000,-46.7500,-47.5000,-47.7500,-48.0000,  -48.75, -49.333,  -49.833,   -50.25,  -50.5838, -50.9167, -50.9167, -50.6667, -50.4167,  -50,  -49,    -46,  -46,  -46,  -46,  -49,  -49.75,   -50,     -50,   -50,   -50,   -50, -56.0838, -56.3333, -56.5000, -56.6667, -57.3334, -58.0000, -58.2500, -58.25, -58.0000, -57.2500, -56.500, -55.7500,  -55, -54.25,  -54,   -54.25,   -54.75, -55.25,  -55.75,   -57.5,-58.25, -58.75, -59.25, -59.5,  -59.75, -59.5, -59.25, -58.75,  -58, -56.5, -54,   -45, -44, -44, -44, -43.5, -43.00,  -43,  -43, -43, -44.25};
    int index;
    float testLo, testHi;
    float x[], y[];
    float m[], c[];
    float dy,dx;
    public IceZone() {
        x = new float[longitudes.length*2];
        y = new float[latitudes.length*2];
        m = new float[x.length-1];
        c = new float[x.length-1];
        for (int i = 0; i < longitudes.length; i++) {
            x[i] = longitudes[i] * (float) phys.radiansPerDegree -  2 * (float) Math.PI;
            x[i+longitudes.length] = x[i] +  2 * (float) Math.PI;
            y[i] = (float) (latitudes[i] * (float) phys.radiansPerDegree);
            y[i+longitudes.length] = y[i];
        }
        for (int i = 0; i < x.length - 1; i++) {
            dx = x[i+1] - x[i];
            dy = y[i+1] - y[i];
            m[i] = dy/dx;
            c[i] = y[i] - m[i]*x[i];
        }
        index = 0;
        testLo = x[index];
        testHi = x[index+1];
    }
    public float SpeedFactor(Vector2 position) {
        while(position.x < testLo) {
            index--;
            testLo = x[index];
            testHi = x[index+1];
        }
        while (position.x > testHi) {
            index++;
            testLo = x[index];
            testHi = x[index+1];
        }
        float diff = position.y - (position.x*m[index] + c[index]);
        if( diff > 0) return 1.0f;
        return 0.3f;
    }

    void draw(Graphics2D g, Mercator screen) {
        g.setColor(Color.red);
        for (int i = 0; i < x.length-1; i++) {
            Vector2 p = screen.fromLatLngToPoint(y[i]*phys.degrees, x[i]*phys.degrees);
            Vector2 p1 = screen.fromLatLngToPoint(y[i+1]*phys.degrees, x[i+1]*phys.degrees);
            g.drawLine((int) p.x, (int) p.y,  (int) p1.x, (int) p1.y);
        }
    }
}
