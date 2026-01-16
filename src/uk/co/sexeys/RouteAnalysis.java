package uk.co.sexeys;

import uk.co.sexeys.JIM.Agent;
import uk.co.sexeys.JIM.JIM;
import uk.co.sexeys.waypoint.Depart;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;


public class RouteAnalysis extends JFrame {
    private final RouteAnalysis jframe;
    RoutePanel routePanel;
    JIM jim;
    Waves waves;

    RouteAnalysis (JIM jim,Waves waves){
        super();
        jframe = this;
        this.jim = jim;
        this.waves = waves;
        SimpleDateFormat format = new SimpleDateFormat("EEE yyyy.MM.dd HH:mm zzz");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        jframe.setTitle("Route Analysis "+format.format(((Depart)jim.boat.waypoints[0]).time));

        setSize(1000, 1000);
//        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        routePanel = new RoutePanel();
        routePanel.setFocusTraversalKeysEnabled(false);
        Container c = getContentPane();
        c.add(routePanel, "Center");
    }

    class RoutePanel extends JPanel {
        BufferedImage image = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);

        RoutePanel() {
            UpdateGraphics();
            setFocusable(true);
            requestFocusInWindow();
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int width = e.getComponent().getWidth();
                    int height = e.getComponent().getHeight();
                    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    UpdateGraphics();
                    repaint();
                }
            });
            addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {

                }

                @Override
                public void keyPressed(KeyEvent e) {
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    float time;
                    switch (e.getKeyChar()) {
                        case 19: // CTRL-s
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
                            Date date = new Date(System.currentTimeMillis());
                            File imageFile = new File(Main.root + "routeshot_" + formatter.format(date) + ".png");
                            System.out.println("Saving screenshot: " + imageFile.getName());
                            try {
                                ImageIO.write(image, "png", imageFile);
                            } catch (
                                    IOException ioException) {
                                ioException.printStackTrace();
                            }
                    }
                }
            });
        }
        void UpdateGraphics() {
            Graphics g = image.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0,0,image.getWidth(),image.getHeight());
            Graphics2D g2d = (Graphics2D) g;
            Font font = new Font("Arial", Font.PLAIN, Main.fontSize);
            g2d.setColor(Color.BLACK);
            g2d.setFont(font);

            int[] TWAData = new int[19];
            int[] sails = new int[jim.boat.polar.polars.size()];
            final ArrayList<Double> TWAValues = new ArrayList<>();
            final ArrayList<Double> TWSValues = new ArrayList<>();
            final ArrayList<Float> HeadingValues = new ArrayList<>();
            final ArrayList<Boolean> PortValues = new ArrayList<>();
            final ArrayList<Integer> SailValues = new ArrayList<>();
            final ArrayList<Long> timeValues = new ArrayList<>();
            final ArrayList<Float> wavesValues = new ArrayList<>();


            Agent Y = jim.keyAgent;
            Vector2 vHeading;
            int count = 0;
            while(Y.previousAgent != null) {
                count ++;
                Vector2 tidalWind = Y.wind.minus(Y.tide);
                double trueWIndSpeed = tidalWind.mag();

                if (trueWIndSpeed == 0) {
                    Y = Y.previousAgent;
                    continue;
                }
                vHeading = new Vector2(Y.heading);

                double cosAngle = -tidalWind.dot(vHeading) / trueWIndSpeed;
                double TWA = Math.acos(cosAngle);
                TWAData[(int)(Math.toDegrees(TWA)/10)]++;
                TWAValues.addFirst(Math.toDegrees(TWA));
                TWSValues.addFirst(trueWIndSpeed);
                PortValues.addFirst(Y.portTack);
                SailValues.addFirst(Y.sail);
                timeValues.addFirst(Y.time);
                wavesValues.add(waves.getValue(Y.position.scale(phys.degrees),Y.time));

                float h = Y.heading % 360;
                if ( h < 0) h += 360;
                HeadingValues.addFirst(h);
                sails[Y.sail]++;
                // TODO integrate engine times
                Y = Y.previousAgent;
            }

            g2d.setColor(Color.BLACK);

            AffineTransform orig = g2d.getTransform();
            g2d.rotate(-Math.PI/2);
            g.drawString("TWA", -100, 3*Main.fontSize);
            g.drawString("TWS", -300, 3*Main.fontSize);
            g.drawString("Heading", -500, 3*Main.fontSize);
            g2d.setTransform(orig);


            Stroke solid2 = new BasicStroke(3);
            Stroke solid1 = new BasicStroke(1);

            g2d.setStroke(solid1);

            int x = 3*Main.fontSize;
            for (Boolean p:PortValues) {
                int xNext = x + 1;
                if (p)
                    g2d.setColor(Color.RED);
                else
                    g2d.setColor(Color.GREEN);
                g.drawRect(x,0,1,180);
                x = xNext;
            }
            x = 3*Main.fontSize;
            for(int i = 30; i <= 180; i+=30) {
                g2d.setColor(Color.BLACK);
                g.drawString(i+"", 0, i);
                g2d.setColor(Color.GRAY);
                g.drawLine(x,i,x+PortValues.size(),i);
            }
            g2d.setColor(Color.BLACK);
            g2d.setStroke(solid2);
            int y = (int) ((double) TWAValues.getFirst());
            for (double a:TWAValues) {
                int xNext = x + 1;
                int yNext = (int) (a);
                g.drawLine(x,y,xNext,yNext);
                x = xNext; y = yNext;
            }
            for(int i = 0; i < TWAData.length; i++)
                g.drawLine(x,i*10,x+(TWAData[i]*300)/count,i*10);

            int offset = 400;

            x = 3*Main.fontSize;
            for (float w:wavesValues) {
                int xNext = x + 1;
                if (w> Main.waveWarning)
                    g2d.setColor(Color.RED);
                else
                    g2d.setColor(Color.WHITE);
                g.drawRect(x,offset-160,1,360);
                x = xNext;
            }

            g2d.setColor(Color.RED);
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.UK);
            formatter.format("Waves > %.0f m",Main.waveWarning);
            g.drawString(sb.toString(),x, offset);

            x = 3*Main.fontSize;
            y = (int) (200+HeadingValues.getFirst());
            g2d.setStroke(solid1);

            for(int i = 0; i <= 360; i+=60) {
                g2d.setColor(Color.BLACK);
                g.drawString(i+"", 0, offset + i/2);
                g2d.setColor(Color.GRAY);
                g.drawLine(x,offset + i/2,x+HeadingValues.size(),offset + i/2);
            }
            g2d.setColor(Color.BLACK);
            g2d.setStroke(solid2);

            for (double h:HeadingValues) {
                int xNext = x + 1;
                int yNext = offset+(int) (h)/2;
                g.drawLine(x,y,xNext,yNext);
                x = xNext; y = yNext;
            }

            offset = 350;


            x = 3*Main.fontSize;
            for (int s:SailValues) {
                int xNext = x + 1;
                if (0== s)
                    g2d.setColor(Color.BLUE);
                else if(1==s)
                    g2d.setColor(Color.RED);
                else
                    g2d.setColor(Color.WHITE);
                g.drawRect(x,offset-150,1,150);
                x = xNext;
            }

            g2d.setColor(Color.BLACK);
            for (int i = 0; i < sails.length ; i++) {
                if (0== i)
                    g2d.setColor(Color.BLUE);
                else if(1==i)
                    g2d.setColor(Color.RED);
                else
                    g2d.setColor(Color.BLACK);
                g.drawString(jim.boat.polar.polars.get(i).name+": "+(sails[i]*100)/count +"%",x, offset -i *50);
            }
            x = 3*Main.fontSize;
            g2d.setStroke(solid1);

            for(int i = 0; i <= 30; i+=5) {
                g2d.setColor(Color.BLACK);
                g.drawString(i+"", 0, offset - i*5);
                g2d.setColor(Color.GRAY);
                g.drawLine(x,offset - i*5,x+HeadingValues.size(),offset - i*5);
            }
            g2d.setColor(Color.BLACK);
            g2d.setStroke(solid2);

            y = (int) (offset+TWSValues.getFirst());
            for (double s:TWSValues) {
                int xNext = x + 1;
                int yNext = offset-(int) (s*phys.knots*5);
                g.drawLine(x,y,xNext,yNext);
                x = xNext; y = yNext;
            }
            SimpleDateFormat format = new SimpleDateFormat("EEE \nyyyy.MM.dd \nHH:mm zzz");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            long startTime = ((Depart)jim.boat.waypoints[0]).time;
            g.drawString(format.format(startTime),3*Main.fontSize,580+Main.fontSize);
            g.drawString("+" + ((jim.keyAgent.time - startTime ) / phys.msPerHour)+"hours",x,580+2*Main.fontSize);

            long lastVadlidForecast = jim.wind.GetLastValidForecast();
            x = 3*Main.fontSize;
            if(lastVadlidForecast > 0) {
                for (long t : timeValues) {
                    if (t > lastVadlidForecast)
                        break;
                    x++;
                }
                g2d.setColor(Color.RED);
                g2d.drawLine(x,0,x,600);
            }
        }

        double scaling = 1.0;
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g;
            g.drawImage(image,0,0,null);
        }
    }
}
