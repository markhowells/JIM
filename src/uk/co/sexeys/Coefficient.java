package uk.co.sexeys;

import java.util.List;

/**
 * Created by Jim on 20/07/2017.
 *
 */
class Coefficient {
    double[] value;
    double[] parameter;
    private double lastP = -1;
    private double lastValue;
    private int lastIndex = 0;
    Coefficient() {}

    void setCoefficient (double[] v, double[] p) {
        value = v;
        parameter = p;
        lastIndex = 0;
        lastValue = interpolate(p[0]);
    }

    Coefficient(List<Vector2> vector2List) {
        value = new double[vector2List.size()];
        parameter = new double[vector2List.size()];
        for (int i = 0; i < vector2List.size(); i++) {
            value[i] = vector2List.get(i).y;
            parameter[i] = vector2List.get(i).x;
        }
        lastIndex = 0;
        lastValue = interpolate(parameter[0]);
    }



    Coefficient(double[] v, float[] p) {
        value = v;
        parameter = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            parameter[i] = p[i];
        }
        lastIndex = 0;
        lastValue = interpolate(p[0]);
    }

    Coefficient (double[] v, double[] p) {
        value = v;
        parameter = p;
        lastIndex = 0;
        lastValue = interpolate(p[0]);
    }

    Coefficient(Coefficient c) {
        value = c.value.clone();
        parameter = c.parameter.clone();
        lastIndex = c.lastIndex;
        lastValue = interpolate(parameter[0]);
    }


    Coefficient ( int[] va, int[] pa ) {
        value = new double[va.length];
        parameter = new double[pa.length];
        for (int i = 0; i < va.length; i++)
            value[i] = (double) va[i] /1000.0;
        for (int i = 0; i < pa.length; i++)
            parameter[i] = (double) pa[i] /1000.0;
        lastValue = interpolate(0);
    }

    double interpolate (double p) {
        if (p == lastP)
            return lastValue;
        if(p < parameter[lastIndex])
            while (p < parameter[lastIndex])
                lastIndex --;
        else if(p > parameter[lastIndex+1])
            while (p > parameter[lastIndex+1])
                lastIndex ++;
        lastP = p;
        lastValue = (p - parameter[lastIndex]) * (value[lastIndex+1] - value[lastIndex]) /
                (parameter[lastIndex+1] - parameter[lastIndex]) + value[lastIndex];
        return lastValue;
    }
}

