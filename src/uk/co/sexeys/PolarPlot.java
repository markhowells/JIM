package uk.co.sexeys;

import javax.swing.*;
import java.awt.*;


/**
 * Created by Jim on 16/07/2018.
 *
 */
class PolarPlot extends JFrame {

    PolarPlot(BiLinear data) {
        super("Polar Plot Frame");

        PolarPanel polarPanel;

        setSize(500, 500);

        polarPanel = new PolarPanel(data);

        Container c = getContentPane();

        c.add(polarPanel, "Center");
    }
    double scale = 50;
    private class PolarPanel extends JPanel {
        BiLinear data;
        double circles[] = new double[] {.5,1,2.5,5,10};
        PolarPanel (BiLinear d) {
            data = d;
        }
        protected void paintComponent(Graphics g1) {
            super.paintComponent(g1);

            Graphics2D g = (Graphics2D) g1;
            g.setColor(Color.lightGray);
            for (double c:circles) {
                int[] x = new int[19];
                int[] y = new int[x.length];
                for (int i = 0; i < x.length; i++) {
                    y[i] = 250-(int) (MathUtil.cosd(i*10) * c*scale);
                    x[i] = (int) (MathUtil.sind(i*10) * c*scale);
                }
                g.drawPolyline(x,y,x.length);
            }
            g.drawLine(0,250,250,0);
            g.drawLine(0,250,500,0);
            g.drawLine(0,250,500,250);

            g.setColor(Color.black);
            for (double c:circles) {
                int[] x = new int[181];
                int[] y = new int[x.length];
                Vector2 v = new Vector2();
                for (int i = 0; i < x.length; i++) {
                    try {
                        data.interpolate((float) c,(float) MathUtil.cosd(i),v);
                    } catch (Exception e) {
                        continue;
                    }
                    y[i] = 250-(int) (v.x*scale);
                    x[i] = (int) (v.y*scale);
                }
                g.drawPolyline(x,y,x.length);
            }
//            for (Vector2[] polar: data.value) {
//                int[] x = new int[polar.length];
//                int[] y = new int[polar.length];
//                int i = 0;
//
//                for (Vector2 p : polar) {
//                    y[i] = 250-(int) (p.x*scale);
//                    x[i++] = (int) (p.y*scale);
//                }
//                g.drawPolyline(x,y,x.length);
//            }

        }

    }
}
