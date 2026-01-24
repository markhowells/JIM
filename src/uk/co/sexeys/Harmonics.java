package uk.co.sexeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import static java.lang.System.exit;

/**
 * Created by Jim on 22/02/2018.
 *
 */
public class Harmonics {
    int num_csts;
    int first_year;
    double[] m_cst_speeds;
    private Units[] known_units;

    double[][] m_cst_epochs, m_cst_nodes;

    class StationData {
        String station_name;
        char station_type;
        double[] amplitude;
        double[] epoch;
        int meridian;
        int zone_offset;
        String tzfile;
        double DATUM;
        String unit;
        boolean have_BOGUS;
        double units_conv;
        String units_abbrv;
    }

    private String fileName;

    public Harmonics(String fileName) {
        this.fileName = fileName;
        known_units = new Units[4];
        known_units[0] = new Units("feet",      "ft",       LENGTH,     0.3048);
        known_units[1] = new Units("meters",    "m",        LENGTH,     1.0);
        known_units[2] = new Units("knots",     "kt",       VELOCITY,   1.0);
        known_units[3] = new Units("knots^2",   "kt^2",     BOGUS,      1.0);
        try {
            read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void read () throws IOException {
        int num_epochs, num_nodes;

        FileInputStream is = new FileInputStream(fileName);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        String data = new String(buffer);
        Scanner s = new Scanner(data);

        read_next_line(s);

        num_csts = s.nextInt();

        if (num_csts <= 0 || num_csts > 1000000)
            throw new IOException("HARMONIC.txt is corrupt - num_csts");

        m_cst_speeds =  new double[num_csts];

        read_next_line(s);
        for (int a=0; a<num_csts; a++) {
            s.next();
            m_cst_speeds[a] = s.nextDouble();
            m_cst_speeds[a] *= Math.PI / 648000; // Convert to radians per second
            s.nextLine();
        }

        read_next_line(s);
        first_year = s.nextInt();

        read_next_line(s);
        num_epochs = s.nextInt();
        s.nextLine();

        if (num_epochs <= 0 || num_epochs > 1000000)
            throw new IOException("HARMONIC.txt is corrupt - epochs");

        m_cst_epochs = new double[num_csts][num_epochs];

        for (int i=0; i<num_csts; i++)
        {
            while(s.nextLine().length() == 0) {}
            for(int b = 0;b<num_epochs; b++) {
                try {
                    m_cst_epochs[i][b] = s.nextDouble() * Math.PI / 180.0;
                } catch (Exception e) {
                    throw new IOException("HARMONIC.txt is corrupt - num_nodes");
                }
            }
        }
        read_next_line(s);

        num_nodes = s.nextInt();
        s.nextLine();

        if (num_nodes <= 0 || num_nodes > 1000000)
            throw new IOException("HARMONIC.txt is corrupt - num_nodes");

        m_cst_nodes = new double[num_csts][num_nodes];

        for (int i=0; i<num_csts; i++) {
            while(s.nextLine().length() == 0) {}
            for(int b = 0;b<num_nodes; b++) {
                try {
                    m_cst_nodes[i][b] = s.nextDouble();
                } catch (InputMismatchException e) {
                    throw new IOException("HARMONIC.txt is corrupt - num_nodes");
                }
            }
        }
    }

    Pattern p = Pattern.compile("#");

    private void read_next_line (Scanner s) throws IOException {
        String line;
        do {
            line = s.nextLine();
        } while ( s.hasNext(p) || line.length() ==0 );
    }

    private static int hhmm2seconds (String hhmm)
    {
        int h, m;
        String[] token = hhmm.split(":",2);
        h = Integer.valueOf(token[0]);
        m = Integer.valueOf(token[1]);

        if (h < 0)
            m *= -1;
        return h*3600 + m*60;
    }

    private static final int LENGTH = 0, VELOCITY = 1, BOGUS = 2;
    private class Units {
        String name;
        String abbrv;
        int type;
        double conv_factor;
        Units(String name, String abbreviation, int type,double conversionFactor) {
            this.name = name;
            abbrv = abbreviation;
            this.type = type;
            conv_factor = conversionFactor;
        }
    }

    /* Find a unit; returns -1 if not found. */
    private int findunit(String unit)
    {
        for (int a=0; a<known_units.length; a++) {
            if (unit.compareTo(known_units[a].name)== 0  ||
                    unit.compareTo(known_units[a].abbrv)== 0)
                return a;
        }
        return -1;
    }

    StationData FindStation(String name) throws IOException {
        FileInputStream is = new FileInputStream(fileName);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        String data = new String(buffer);
        int i = data.toLowerCase().indexOf("\n"+name.toLowerCase());
        Scanner s;
        if( i > 0 ) {
            s = new Scanner(data.substring(i+1));
            s.nextLine();
        }
        else
            return null;

        StationData psd;
        psd = new StationData();
        psd.station_name = name;

        psd.amplitude = new double[num_csts];
        psd.epoch = new double[num_csts];

        psd.meridian = hhmm2seconds (s.next());
        psd.zone_offset = 0;
        psd.tzfile = s.nextLine();

        psd.DATUM = s.nextDouble();
        psd.unit = s.next();
        if(psd.unit== null)
            psd.unit = "unknown";
        psd.have_BOGUS = (findunit(psd.unit) != -1) && (known_units[findunit(psd.unit)].type == BOGUS);

        int unit_c;
        if (psd.have_BOGUS)
            unit_c = findunit("knots");
        else
            unit_c = findunit(psd.unit);

        if (unit_c != -1) {
            psd.units_conv = known_units[unit_c].conv_factor;
            psd.units_abbrv = known_units[unit_c].abbrv;
        }
        if(psd.unit.equalsIgnoreCase("knots"))
            psd.station_type = 'C';
        else
            psd.station_type = 'T';
        psd.DATUM *= psd.units_conv;
        for (int a=0; a<num_csts; a++) {
            try {
                s.next();
                psd.amplitude[a] = s.nextDouble() * psd.units_conv;
                psd.epoch[a] = s.nextDouble() * Math.PI / 180.;
            } catch (InputMismatchException e) {
                System.out.println(psd.station_name+" only "+a+" records...");
                break;
            }
        }
        return psd;
    }

    int FindTZ(String name) throws IOException {
        if (name.isEmpty())
            throw new IOException("Name is empty.");
        FileInputStream is = new FileInputStream(fileName);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        String data = new String(buffer);
        int i = data.indexOf(name+"\n");
        Scanner s;
        if( i > 0 ) {
            s = new Scanner(data.substring(i-20));
            s.nextLine();
            String line = s.nextLine();
            String[] fields = line.split(" ");
            String[] tokens = fields[0].split(":");
            int hour = Integer.parseInt(tokens[0]);
            int minute = Integer.parseInt(tokens[1]);
            return (int) ((Math.abs(hour) * 60 + minute) * Math.signum(hour));
        }
        else
            throw new IOException("Name not found.");
    }
    PlotHeight plotHeight;
    void Plot(IDX.IDX_entry idx, Calendar time) {
        plotHeight = new PlotHeight(idx,time);
        plotHeight.setVisible(true);
    }


    class PlotHeight extends JFrame {
        PlotPanel plotPanel;

        private int mouseX, mouseY;

        private String xAxis= "X", yAxis="Y";

        PlotHeight(IDX.IDX_entry idx, Calendar time) {
            super();
            if( idx.pref_sta_data == null ) {
                try {
                    idx.pref_sta_data = FindStation(idx.IDX_reference_name);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if(idx.pref_sta_data == null) {
                System.out.println("Could not find station:\n"+idx.IDX_reference_name+"\nin\n"+idx.IDX_station_name);
                exit(1);
            }

            Calendar startTime = (Calendar) time.clone();
            startTime.set(Calendar.HOUR_OF_DAY,0);
            startTime.set(Calendar.MINUTE,0);
            startTime.set(Calendar.SECOND,0);

            Calendar localtime = (Calendar) startTime.clone();

            if (idx.IDX_tzname !=null) {
                try {
                    localtime.add(Calendar.MINUTE,FindTZ(idx.IDX_tzname)) ;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else
                localtime.add(Calendar.MINUTE,idx.IDX_time_zone);

            int year = localtime.get(Calendar.YEAR);
            Compute c = new Compute(Harmonics.this, idx ,year);
            long timeUTC = c.timeFromYearStart(localtime);
            Compute.TideData[] t;
            double datum = idx.pref_sta_data.DATUM;
            if(idx.IDX_type == 'T')
                t = c.primary(timeUTC);
            else if(idx.IDX_type == 't') {
                t = c.secondary(timeUTC);
                datum = datum * idx.IDX_lt_mpy - idx.IDX_lt_off;
            }
            else
                return;
            for (Compute.TideData tideData : t)
                tideData.time = (tideData.time - timeUTC);
            PlotPanel pp = new PlotPanel(t,datum, c.minAmplitude, c.maxAmplitude);

            SimpleDateFormat sdf = new SimpleDateFormat("EEE d MMM");
            setTitle(sdf.format(startTime.getTime())+" "+idx.IDX_station_name);
            setSize(500, 500);
            Container container = getContentPane();
            container.add(pp, "Center");

            long riseTime=0, setTime;
            Calendar cal = (Calendar) startTime.clone();
            Vector2 location = new Vector2(Math.toRadians(idx.IDX_lon), Math.toRadians(idx.IDX_lat));
            riseTime = Sun.riseTime(cal,location);
            setTime = Sun.setTime(cal,location);

            sdf = new SimpleDateFormat("HH:mm zzz");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(riseTime);
            System.out.println("\nRise time: "+ sdf.format(cal.getTime()));
            cal.setTimeInMillis(setTime);
            System.out.println("Set time: "+sdf.format(cal.getTime()));
        }

        class PlotPanel extends JPanel {
            double minX=0,maxX=24*3600,minY,maxY;
            Compute.TideData[] data;
            double datum;

            PlotPanel(Compute.TideData[] data, double datum, double LAT, double HAT) {
                setVisible(true);
                this.data = data;
                minY = LAT;
                maxY = HAT;
                this.datum = datum;

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
                int[] x = new int[data.length];
                int[] y = new int[data.length];
                int i = 0;
                for (Compute.TideData d:data) {
                    y[i] = (int) ((maxY - d.data) * sy);
                    x[i] = (int) ((d.time - minX) * sx);
                    i++;
                }
                g.drawPolyline(x, y, x.length);
                g.drawLine((int) minX,(int) ((maxY + datum) * sy),(int) maxX,(int) ((maxY + datum) * sy));


                double mX = mouseX/sx +minX;
                double mY = maxY - mouseY/sy;
                int hour = (int) (mX/3600.);
                int minute = (int) ((mX % 3600.) / 60.);

                StringBuilder sb = new StringBuilder();
                Formatter formatter = new Formatter(sb, Locale.UK);
                //            ScatterPlot pp = new ScatterPlot(data,"Time UTC","",);

                formatter.format("HAT     %2d:%2d UTC Height (LAT) %.2f m",hour,minute,mY-minY);
                Font font = new Font("Arial", Font.BOLD, 20);
                g.setFont(font);
                g.drawString(sb.toString(), 0, 20);
                g.drawString("LAT",0,height);
                g.drawString("Datum",0,(int) ((maxY + datum) * sy));

            }
        }
    }
}




