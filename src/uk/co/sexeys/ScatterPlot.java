package uk.co.sexeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;


/**
 * Created by Jim on 23/07/2018.
 *
 */
class ScatterPlot extends JFrame {
    ScatterPanel scatterPanel;

    private int mouseX, mouseY;

    private String xAxis= "X", yAxis="Y";

    ScatterPlot(Coefficient data[], String x, String y) {
        super("Scatter Plot Frame");
        init(data);
        xAxis = x;
        yAxis = y;
    }

    ScatterPlot(Coefficient data[], String x, String y, String t) {
        super(t);
        init(data);
        xAxis = x;
        yAxis = y;
    }

    ScatterPlot(List<Vector2> data, String xAxis, String yAxis, String t) {
        super(t);
        Coefficient[] coefficient = new Coefficient[1];
        coefficient[0] = new Coefficient(data);
        init(coefficient);
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }

    ScatterPlot(Coefficient data[]) {
        super("Scatter Plot Frame");
        init(data);
    }

    private void init(Coefficient data[]) {
        setSize(500, 500);
        scatterPanel = new ScatterPanel(data);
        Container c = getContentPane();
        c.add(scatterPanel, "Center");
    }
    public void Update(List<Vector2> data) {
        scatterPanel.data = new Coefficient[1];
        scatterPanel.data[0] = new Coefficient(data);
        scatterPanel.repaint();
    }
    class ScatterPanel extends JPanel {
        Coefficient data[];
        double minX=-180,maxX=180,minY=-2,maxY=2;

        ScatterPanel(Coefficient d[]) {
            data = d;
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    super.mouseMoved(e);
                    mouseX = e.getX();
                    mouseY = e.getY();
                    repaint();
                }
            });
        }

        protected void paintComponent(Graphics g1) {
            super.paintComponent(g1);

            int width = getWidth();
            int height = getHeight();
            double sx = width / (maxX - minX);
            double sy = height / (maxY - minY);

            Graphics2D g = (Graphics2D) g1;
            for (Coefficient d:data) {
                int[] x = new int[d.value.length];
                int[] y = new int[d.value.length];

                for (int i=0; i < d.value.length; i++) {
                    y[i] = (int) ((maxY - d.value[i]) * sy);
                    x[i] = (int) ((d.parameter[i] - minX) * sx);
                }
                g.drawPolyline(x, y, x.length);
            }

            double mX = mouseX/sx +minX;
            double mY = maxY - mouseY/sy;

            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.UK);
            formatter.format("%s: %.2f %s: %.2f",xAxis,mX,yAxis,mY);
            Font font = new Font("Arial", Font.BOLD, 20);
            g.setFont(font);
            g.drawString(sb.toString(), 20, 20);
        }

        void setLimits() {
            minX = minY = Double.MAX_VALUE;
            maxX = maxY = Double.MIN_VALUE;
            for (Coefficient d:data) {
                for(double v:d.parameter){
                    if (v < minX)
                        minX = v;
                    if ( v > maxX)
                        maxX = v;
                }
                for(double v:d.value){
                    if (v < minY)
                        minY = v;
                    if ( v > maxY)
                        maxY = v;
                }
            }
        }
        void setLimits(double m) {
            setLimits();
            minX = m;
        }
    }
}
