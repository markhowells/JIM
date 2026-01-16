package uk.co.sexeys.water;

import uk.co.sexeys.Mercator;
import uk.co.sexeys.Vector2;

import java.awt.*;


public abstract class Water {
    static final int arrowSize = 20;
    public enum SOURCE {
        UNKNOWN,
        TIDE,
        CURRENT,
        PREVAILING,
        FIXED
    }
    public static String Source(SOURCE s){
        if ( s == SOURCE.UNKNOWN )
            return "UNKNOWN";
        if ( s == SOURCE.TIDE )
            return "TIDE";
        if ( s == SOURCE.CURRENT )
            return "CURRENT";
        if ( s == SOURCE.PREVAILING )
            return "PREVAILING";
        return "FIXED";
    }


    Water() {}

    public SOURCE getValue(final Vector2 p, final long t, final Vector2 value) {
        value.x = value.y = 2; // Some test values
        return SOURCE.FIXED;
    }

    SOURCE getSOurce(int index){
        return SOURCE.FIXED;
    }

    static float[] includeDateLine (float[] d, int stride) {
        float[] temp = new float[d.length*2];
        int i = 0;
        int rowEnd = stride;
        int midRow = stride/2;
        int rowStart = 0;
        do{
            for (int j = midRow; j < rowEnd; j++) {
                temp[i++] = d[j];
            }
            for (int j = rowStart; j < rowEnd; j++) {
                temp[i++] = d[j];
            }
            for (int j = rowStart; j < midRow; j++) {
                temp[i++] = d[j];
            }
            rowEnd += stride;
            midRow += stride;
            rowStart += stride;
        } while ( i < temp.length);
        return temp;
    }

    public long GetLastValidForecast() {
        return -1;
    }

    public void draw(Graphics2D g, Mercator screen, long time) {
        double dx = (screen.bottomRight.x - screen.topLeft.x)/20;
        double dy = (screen.topLeft.y - screen.bottomRight.y)/20;
        Vector2 v = new Vector2();
        SOURCE source;
        g.setColor(Color.blue);
        for (double i = screen.topLeft.x+dx; i < screen.bottomRight.x; i+= dx) {
            for (double j = screen.bottomRight.y+dy; j < screen.topLeft.y; j+= dy) {
                Vector2 p = screen.fromLatLngToPoint(j, i);
                source = getValue(new Vector2(i,j),time,v);

                if (source != SOURCE.UNKNOWN) {
                    float magnitude = (float) Math.sqrt(v.x*v.x + v.y*v.y);
                    if (0 == magnitude)
                        continue;
                    if(magnitude <0.2) {
                        g.setStroke(new BasicStroke(1));
                        g.drawLine((int) p.x, (int) p.y,
                                (int) (p.x + v.x * arrowSize/0.2), (int) (p.y - v.y * arrowSize/0.2));
                    }
                    else {
                        g.setStroke(new BasicStroke((int)(magnitude*5)));
                        g.drawLine((int) p.x, (int) p.y,
                                (int) (p.x + v.x * arrowSize/magnitude), (int) (p.y - v.y * arrowSize/magnitude));
                    }
//                        g.fillRect((int) p.x - 2, (int) p.y - 2, 4, 4);
                }
            }
        }
        g.setStroke(new BasicStroke(1));
    }


//    public void draw(Graphics2D g, Mercator screen, long time) {
//        double dx = (screen.bottomRight.x - screen.topLeft.x)/20;
//        double dy = (screen.topLeft.y - screen.bottomRight.y)/20;
//        Vector2 v = new Vector2();
//        int index;
//        g.setColor(Color.black);
//        for (double i = screen.topLeft.x+dx; i < screen.bottomRight.x; i+= dx) {
//            for (double j = screen.bottomRight.y+dy; j < screen.topLeft.y; j+= dy) {
//                Vector2 p = screen.fromLatLngToPoint(j, i);
//                SOURCE source = SOURCE.UNKNOWN;
//                try {
//                    index = getValue(new Vector2(i,j),time,v,1);
//                    source = getSOurce(index);
//                } catch (Exception e) {
//                    continue;
//                }
//                if (index > 0) {
//                    if (source == SOURCE.PREVIOUS) {
//                        g.setColor(Color.blue);
//                    } else if (source == SOURCE.LATEST) {
//                        g.setColor(Color.black);
//                    } else if (source == SOURCE.WEIGHTED) {
//                        g.setColor(Color.darkGray);
//                    } else if (source == SOURCE.PREVAIOING) {
//                        g.setColor(Color.green);
//                    } else {
//                        g.setColor(Color.red);
//                    }
//                    g.drawLine((int) p.x, (int) p.y,
//                            (int) (p.x + v.x* arrowSize), (int) (p.y - v.y* arrowSize));
//                    g.fillRect((int) p.x-1, (int) p.y-1, 2, 2);
//                }
//            }
//        }
//    }
    public long GetTime(int record) {
        return 0;
    }
}
