package uk.co.sexeys.wind;


import uk.co.sexeys.Main;
import uk.co.sexeys.Vector2;
import uk.co.sexeys.phys;

import java.io.*;
import java.util.Calendar;
import java.util.TimeZone;

public class Prevailing extends Wind{
    //    private final ArrayList<Record> data = new ArrayList<>();
    private Record[] data = new Record[12];
    private long referenceTime;
    private static long msPerMonth = (long) (phys.msPerDay * 365.25 / 12); // Crude estimate
    private final Vector2 a = new Vector2();
    private final Vector2 b = new Vector2();

    int arrowSize = 20;

    static class Record {
        float[] u, v;
        int stride;
        float top, left, bottom, right, fx, fy;

        Record(Record c) {
            u = c.u.clone();
            v = c.v.clone();
            stride = c.stride;
            left = c.left;
            right = c.right;
            top = c.top;
            bottom = c.bottom;
            fx = c.fx;
            fy = c.fy;
        }

        Record() {
        }

        Record(float ub, float vb) {
            u = new float[]{ub, ub, ub, ub};
            v = new float[]{vb, vb, vb, vb};
            stride = 2;
            left = -190;
            right = 180;
            bottom = -90; // somewhere we cannot get
            top = -89;
            fx = (stride - 1) / (right - left);
            fy = (u.length / stride - 1) / (top - bottom);
        }

        void getValue(Vector2 p, Vector2 out) { //TODO very slow in inner loop
            final float dlon = p.x - left;
            final float x = (dlon * fx) % 180.0f;
            final float dlat = p.y - bottom;
            float y = dlat * fy;

            final int i = (int) x;
            final int j = (int) y;
            final int offset00 = i + j * stride;
            final int offset10 = offset00 + 1;
            final int offset01 = offset00 + stride;
            final int offset11 = offset01 + 1;
            final float dx = x - i;
            final float dy = y - j;
            final float dx1 = 1 - dx;
            float dy1 = 1 - dy;
            final float A = dx1 * dy1;
            float B = dx * dy1;
            float C = dx1 * dy;
            float D = dx * dy;
            out.x = (u[offset00] * A + u[offset10] * B + u[offset01] * C + u[offset11] * D);
            out.y = (v[offset00] * A + v[offset10] * B + v[offset01] * C + v[offset11] * D);
        }
    }
    void AddPrevailing(){} // Not used
//    public int getValue(Vector2 p, long t, Vector2 value, int dump) {
//        return getValue(p,t,value);
//    }
    public SOURCE getValue(Vector2 p, long t, Vector2 value) {
        long deltaTime = t - referenceTime;
        int monthBefore = (int) (deltaTime / msPerMonth) % 12;
        int monthAfter = (monthBefore + 1) % 12;

        data[monthAfter].getValue(p, a);
        data[monthBefore].getValue(p, b);
        final double dt = msPerMonth;
        final double df = t - (referenceTime + monthBefore * msPerMonth);
        final double j = df / dt;
        if ((j < 0) || (j > 1))
            System.out.println("Prevailing wind j error");
        final double A = (1 - j);

        value.x = (float) (a.x * A + b.x * j);
        value.y = (float) (a.y * A + b.y * j);
        return SOURCE.PREVAILING;
    }

//    void draw(Graphics2D g, Mercator screen, long time) {
//        double dx = (screen.bottomRight.x - screen.topLeft.x) / 20;
//        double dy = (screen.topLeft.y - screen.bottomRight.y) / 20;
//        Vector2 v = new Vector2();
//        g.setColor(Color.darkGray);
//        for (double i = screen.topLeft.x + dx; i < screen.bottomRight.x; i += dx) {
//            for (double j = screen.bottomRight.y + dy; j < screen.topLeft.y; j += dy) {
//                Vector2 p = screen.fromLatLngToPoint(j, i);
//                getValue(new Vector2(i, j), time, v);
//                float magnitude = (float) Math.sqrt(v.x * v.x + v.y * v.y);
//                if (0 == magnitude)
//                    continue;
//                if (magnitude < 0.2) {
//                    g.setStroke(new BasicStroke(1));
//                    g.drawLine((int) p.x, (int) p.y,
//                            (int) (p.x + v.x * arrowSize / 0.2), (int) (p.y - v.y * arrowSize / 0.2));
//                } else {
//                    g.setStroke(new BasicStroke((int) (magnitude * 5)));
//                    g.drawLine((int) p.x, (int) p.y,
//                            (int) (p.x + v.x * arrowSize / magnitude), (int) (p.y - v.y * arrowSize / magnitude));
//                }
//                g.fillRect((int) p.x - 2, (int) p.y - 2, 4, 4);
//            }
//        }
//        g.setStroke(new BasicStroke(1));
//    }

    public Prevailing(long time) {
        String[] uLine;
        String[] vLine;
        BufferedReader uBR;
        BufferedReader vBR;
        BufferedReader wBR;

        try {
            uBR = new BufferedReader(new FileReader(Main.root + "Prevailing" + File.separator + "U_TRENBERTH1.tsv"));
            vBR = new BufferedReader(new FileReader(Main.root + "Prevailing" + File.separator + "V_TRENBERTH1.tsv"));
//            wBR = new BufferedReader(new FileReader(Main.root + "Prevailing" + File.separator + "UT_TRENBERTH1.tsv"));
            for (int i = 0; i < 12; i++) {
                data[i] = new Record();
                data[i].stride = 144 + 1; // for date line calculations
                data[i].bottom = -90;
                data[i].top = 90;
                data[i].left = -180;
                data[i].right = 177.5f + 2.5f; // for date line calculations
                data[i].u = new float[73 * data[i].stride];
                data[i].v = new float[73 * data[i].stride];
                data[i].fx = (data[i].stride - 1) / (data[i].right - data[i].left);
                data[i].fy = (data[i].u.length / data[i].stride - 1) / (data[i].top - data[i].bottom);
                uBR.readLine();
                vBR.readLine();
//                wBR.readLine();
                for (int j = 0; j < 73; j++) {
                    uLine = uBR.readLine().split("\t");
                    vLine = vBR.readLine().split("\t");
                    int m = j * data[i].stride;
                    for (int k = 0; k < data[i].stride -1; k++) {
                        data[i].u[m] = Float.parseFloat(uLine[k + 1]);
                        data[i].v[m] = Float.parseFloat(vLine[k + 1]);
                        m++;
                    }
                    data[i].u[m] = data[i].u[0]; // for date line calculations
                    data[i].v[m] = data[i].v[0];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        Calendar refTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        refTime.setTimeInMillis(time);
        int year = refTime.get(Calendar.YEAR);
        // TODO set reference time from month not from January
        refTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        refTime.set(year, Calendar.JANUARY, 1);
        referenceTime = refTime.getTimeInMillis();
//        System.out.println("Read prevailing winds.");
    }
}


