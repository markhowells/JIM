package uk.co.sexeys;


import uk.co.sexeys.CMap.CMap;
import uk.co.sexeys.JIM.*;
import uk.co.sexeys.water.PrevailingCurrent;
import uk.co.sexeys.water.Tide;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.water.Current;
import uk.co.sexeys.waypoint.*;
import uk.co.sexeys.wind.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;


/**
 * Created by Jim on 04/06/2018.
 * http://www.1yachtua.com/Mediterranean_Maps/mediterranean_atlas_kap.asp
 * https://github.com/SignalK/maptools/blob/master/src/main/java/org/signalk/maptools/KAPParser.java
 */


class StreamFrame extends JFrame {

    Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private final SimpleDateFormat format = new SimpleDateFormat("EEE yyyy.MM.dd HH:mm:ss zzz");
    private final StreamFrame jframe;
    StreamPanel streamPanel;
    String POLAR = "";


    StreamFrame() {
        super("Course2Steer");
        jframe = this;
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        setSize(1500, 1000);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        streamPanel = new StreamPanel();
        streamPanel.setFocusTraversalKeysEnabled(false);
        Container c = getContentPane();
        c.add(streamPanel, "Center");
    }

    class StreamPanel extends JPanel {
        WVS wvs = new WVS(10000000);
        CMap cMap;
        ArrayList<TidalStream> tidalStreams = new ArrayList<>();
        ArrayList<Chart> charts = new ArrayList<>();

        Wind wind;

        boolean showWind = true;
        Water water;
        boolean showWater = Main.useWater;
        Waves waves;
        boolean showWaves = true;


        LastRoute lastRoute = new LastRoute();

        IDX idx = new IDX(Main.root+File.separator+"charts/tides/HARMONIC");
        Harmonics harmonics = new Harmonics(Main.root+File.separator+"charts/tides/HARMONIC");

        Depth depth = new Depth(Main.root+File.separator+"charts/Bathymetry/GEBCO_2020.dat", 4);
        ArrayList<Boat> undoList = new ArrayList<>();
        Boat boat = new Boat();
        JIM jim;
        Mercator screen;

        int startX = -1, startY = -1, endX, endY, lastX, lastY;

        boolean[] keyPressed = new boolean[256];

        Continuous continuous;

        StreamPanel() {
            if(Main.REPLAY.length() != 0)
                replay(this);
            else
                parse(this);

            cMap = new CMap(0);
            setFocusable(true);
            requestFocusInWindow();
            addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if(e.getKeyChar() < keyPressed.length)
                        keyPressed[e.getKeyChar()] = true;
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    if (continuous.running()) {
                        switch(e.getKeyChar()) {
                            case '=':
                            case '+':
                            case '-':
                            case '_':
                            case '1':
                            case '2':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                            case '!':
                            case 'h':
                            case 'q':
                            case 'Q':
                            case 'b':
                            case 'r':
                                return;
                        }
                    }
                    switch(e.getKeyChar()) {
                        case '=':
                        case '+':
                        case '-':
                        case '_':
                            undoList.add(boat);
                            boat = new Boat(boat);
                            switch (e.getKeyChar()) {
                                case '=' -> ((Depart) boat.waypoints[0]).ChangeTime(phys.msPerDay);
                                case '+' -> ((Depart) boat.waypoints[0]).ChangeTime(phys.msPerHour);
                                case '-' -> ((Depart) boat.waypoints[0]).ChangeTime(-phys.msPerDay);
                                case '_' -> ((Depart) boat.waypoints[0]).ChangeTime(-phys.msPerHour);
                            }
                            newRoute();
                            boat.DE.PrintInstructions();
                            UpdateGraphics();
                            repaint();
                            break;
                        case 'p':
                            PolarPlot pp = new PolarPlot(boat.polarToUse);
                            pp.setVisible(true);
                            break;

                        case 't':  // best tack
                            boat.showBestTack = !boat.showBestTack;
                            UpdateGraphics();
                            repaint();
                            break;
                        case 'c': // constant course
                            boat.showConstantCourse = !boat.showConstantCourse;
                            UpdateGraphics();
                            repaint();
                            break;
                        case 'f': // follow wind
                            boat.showConstantTWA = !boat.showConstantTWA;
                            UpdateGraphics();
                            repaint();
                            break;
                        case 'x': // toggle candidates
                            boat.showCandidates = !boat.showCandidates;
                            UpdateGraphics();
                            repaint();
                            break;
                        case 'X': // toggle wind
                            showWind = !showWind;
                            UpdateGraphics();
                            repaint();
                            break;
                        case 24: // ctrl-x toggle tide
                            showWater = !showWater;
                            UpdateGraphics();
                            repaint();
                            break;
                        case '3': // 100 hours JIM
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(Main.C2SLegs,((Depart)boat.waypoints[0]).getTime()+1006*phys.msPerHour, jim,wind, water, boat,  Main.searchTolerance, 1234, boat.polar.raw);
                            UTC.setTimeInMillis(boat.DE.getTime() );
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UpdateGraphics();
                            repaint();
                            break;
                        case '0': // all route JIM
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(Main.C2SLegs,Long.MAX_VALUE,jim,wind, water, boat,  Main.searchTolerance, 1234, boat.polar.VMG);
                            UTC.setTimeInMillis(boat.DE.getTime() );
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UpdateGraphics();
                            repaint();
                            break;
                        case 'a':
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            try {
                                jim.Search(jim.route.currentTime+
                                        jim.route.legs[jim.route.currentLeg].waypoint.timeStep);
                                System.out.println("Cuurent wyapoint "+boat.currentWaypoint);
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UTC.setTimeInMillis(jim.GetTime());
                            UpdateGraphics();
                            repaint();
                            break;
                        case '9': // next 100 iterations
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            DifferentialEvolution oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(oldDE,boat);
                            boat.DE.search(100,0.2f,0);
                            boat.DE.PrintInstructions();
                            UTC.setTimeInMillis(boat.DE.getTime() );
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UpdateGraphics();
                            repaint();
                            break;
                        case '8': // next 100 iterations
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(oldDE,boat);
                            boat.DE.search(100,0.3f,0);
                            boat.DE.PrintInstructions();
                            UTC.setTimeInMillis(boat.DE.getTime() );
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UpdateGraphics();
                            repaint();
                            break;
                        case '7': // next 100 iterations
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(oldDE,boat);
                            boat.DE.search(100,0.4f, 0);
                            boat.DE.PrintInstructions();
                            UTC.setTimeInMillis(boat.DE.getTime());
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UpdateGraphics();
                            repaint();
                            break;
                        case '6': // next 10 iterations
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(oldDE,boat);
                            boat.DE.search(100,0.5f,0);
                            boat.DE.PrintInstructions();
                            UTC.setTimeInMillis(boat.DE.getTime() );
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UpdateGraphics();
                            repaint();
                            break;
                        case 'h': // halve route length (keep-all)
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            long t = boat.DE.getLastFix().time - boat.DE.getInitialFix().time;
                            Fix f = boat.DE.findNearestFix(t/2 + boat.DE.getInitialFix().time);
                            long newResolution = (f.time - boat.DE.getInitialFix().time) / Main.numberOfFixes;
                            oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(oldDE.getLegs(),wind, water,boat,oldDE.depart,new InterimFix(f),newResolution,Main.searchTolerance,1234,boat.polarToUse);

                            boat.DE.generateAgents(oldDE);
                            boat.DE.recomputeErrors();
//                            do{
//                                boat.DE.converge(0.1f,0);
//                            } while (boat.DE.HasAnyFailedSolutions());
//                            boat.DE.PrintInstructions();
//                            UTC.setTimeInMillis(boat.DE.getTime() );
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UpdateGraphics();
                            repaint();
                            break;
                        case 'q': // double the waypoints
                            if( boat.DE.getLegs() >= 50)
                                break;
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(oldDE,boat);
                            boat.DE.generateDoubleAgents(oldDE);
                            boat.DE.recomputeErrors();
//                            do{
//                                boat.DE.converge(Main.DESearchFactor,0);
//                            } while (boat.DE.HasAnyFailedSolutions());
                            UTC.setTimeInMillis(boat.DE.getTime() );
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UpdateGraphics();
                            repaint();
                            break;
                        case 'Q': // toggle rawPolar
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(oldDE,boat);
                            if(boat.polarToUse == boat.polar.raw)
                                boat.DE.setPolar(boat.polar.VMG);
                            else
                                boat.DE.setPolar(boat.polar.raw);

                            boat.DE.recomputeErrors();
//                            boat.DE.converge(Main.DESearchFactor,0);
//                            boat.DE.PrintInstructions();
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UTC.setTimeInMillis(boat.DE.getTime() );
                            UpdateGraphics();
                            repaint();
                            break;
                        case 'A': // Penalty
                            jframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(oldDE,boat);
//                            boat.DE.generateAgents(oldDE);
                            boat.DE.recomputeErrors();
//                            boat.DE.converge(Main.DESearchFactor,0);
//                            boat.DE.PrintInstructions();
                            jframe.setCursor(Cursor.getDefaultCursor());
                            UTC.setTimeInMillis(boat.DE.getTime() );
                            UpdateGraphics();
                            repaint();
                            break;
                        case ' ':
                            Main.continuousFactor = 0.5f;
                            if(continuous.running()) {
                                continuous.stop();
                                jframe.setCursor(Cursor.getDefaultCursor());
                            }
                            else {
                                continuous = new Continuous(streamPanel, boat.DE);
                                jframe.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                                continuous.startRunning();
                            }
                            break;
                        case 'o':
                            Main.continuousFactor = 0.3f;
                            if(continuous.running()) {
                                continuous.stop();
                                jframe.setCursor(Cursor.getDefaultCursor());
                            }
                            else {
                                continuous = new Continuous(streamPanel, boat.DE);
                                jframe.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                                continuous.startRunning();
                            }
                            break;
                        case 'W': {
                            Calendar windTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
//                            windTime.setTimeInMillis(wind.GetTime(0));
                            windTime.setTimeInMillis(((InterimFix) boat.waypoints[0]).time);
//                            windTime.add(Calendar.HOUR, 12); // This is needed otherwise it will step back a forecast

                            newRoute();
                            boat.DE.PrintInstructions();
                            UpdateGraphics();
                            repaint();
                            break;
                        }
                        case '?':
                            JLabel labelHelp = new JLabel("<html>" + ("""
                                            Mouse:
                                               Left - Drag
                                               Shift Left - Measurement tool
                                               Right - toggle chart
                                               Alt Right - toggle tidal stream chart
                                               Wheel - zoom1
                                               Shift wheel - change display time minute
                                               Ctrl Shift wheel - change display time hour
                                            Key presses
                                               [ ] change CMap scale
                                               1/2/3/4 set DE endpoint to 12,24,36 and 48 hours from start
                                               space - toggle live iteration
                                               q - double DE waypoints
                                               Q - toggle raw.VMG polars
                                               h - half the DE route length
                                               r - recompute DE using random waypoints
                                               R - show route analysis window
                                               p - show current polar
                                               x - toggle DE route embellishments
                                               X - toggle wind embellishments
                                               ctrl x - toggle tide embellishments
                                               I - show course to steer 12 hour window
                                               i - show course to steer waypoint window
                                               g - write gpx file to memory card
                                               ctrl-z - undo last calculation
                                               ctrl-s - save screen shot
                                            Key and left mouse button combinations
                                               ALT - display tide height for nearest station
                                               d - recompute current route ending at MIM point nearest to cursor
                                               ctrl-d - recompute current route ending at DE point nearest to cursor
                                               s - recompute current route starting at MIM point nearest to cursor
                                               D - compute new route to cursor
                                            """
                                    ).replaceAll("<","&lt;").
                                    replaceAll(">", "&gt;").
                                    replaceAll("\n", "<br/>").
                                    replaceAll(" ", "&nbsp;") + "</html>");
                            labelHelp.setFont(new Font("Courier New", Font.BOLD, 16));
                            JOptionPane.showMessageDialog(jframe, labelHelp,
                                    "Help", JOptionPane.INFORMATION_MESSAGE);
                            break;
                        case 'R': {
                            RouteAnalysis routeAnalysis = new RouteAnalysis(jim,waves);
                            routeAnalysis.setVisible(true);
                            break;
                        }
                        case 'I': {
                            String text = boat.DE.PrintShortInstructions();
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(new StringSelection(text), null);
                            JLabel label = new JLabel("<html>" + text.
                                    replaceAll("<", "&lt;").
                                    replaceAll(">", "&gt;").
                                    replaceAll("\n", "<br/>").
                                    replaceAll(" ", "&nbsp;") + "</html>");
                            label.setFont(new Font("Courier New", Font.PLAIN, 18));
                            JOptionPane.showMessageDialog(jframe, label,
                                    "Course to steer", JOptionPane.INFORMATION_MESSAGE);

                            break;
                        }
                        case 'i': {
                            String text = boat.DE.PrintInstructions();
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(new StringSelection(text), null);
                            JLabel label = new JLabel("<html>" + text.
                                    replaceAll("<", "&lt;").
                                    replaceAll(">", "&gt;").
                                    replaceAll("\n", "<br/>").
                                    replaceAll(" ", "&nbsp;") + "</html>");
                            label.setFont(new Font("Courier New", Font.PLAIN, Main.fontSize));
                            JOptionPane.showMessageDialog(jframe, label,
                                    "Course to steer", JOptionPane.INFORMATION_MESSAGE);
                            LastRoute lastRoute = new LastRoute(boat.DE);
                            lastRoute.WriteFile();
                            break;
                        }
                        case 'g':

                            File file = new File( "c:\\course2steer.gpx");
                            try {
                                FileWriter fileWriter = new FileWriter(file);
                                fileWriter.write(boat.DE.GPX("Tidal Stream"));
                                fileWriter.close();
                            } catch (IOException e1) {
                                JOptionPane.showMessageDialog(jframe, "Could not write GPX file to E:\\course2steer.gpx",
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                break;
                            }
                            JOptionPane.showMessageDialog(jframe, "Wrote GPX file to E:\\course2steer.gpx",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                            break;
                        case 'G':
                            file = new File( Main.root+"JIM.gpx");
                            try {
                                FileWriter fileWriter = new FileWriter(file);
                                fileWriter.write(jim.GPX("JIM"));
                                fileWriter.close();
                            } catch (IOException e1) {
                                JOptionPane.showMessageDialog(jframe, "Could not write JIM GPX file to E:\\course2steer.gpx",
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                break;
                            }
                            JOptionPane.showMessageDialog(jframe, "Wrote JIM GPX file to E:\\course2steer.gpx",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                            break;
                        case 'u':
                            charts.clear();
                            Chart.addTable(Main.root+File.separator+"Charts", charts);
                            break;
                        case 'r':
                            newResolution = boat.DE.getElapsedTime() / Main.numberOfFixes;
                            oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
//TODO not sure what to do about this
//                            for(Fix w:boat.waypoints){
//                                if(w.position.x > Math.PI)
//                                    w.position.x -= 2*Math.PI;
//                                if(w.position.x < -Math.PI)
//                                    w.position.x += 2*Math.PI;
//                            }
                            if(oldDE.depart instanceof InterimFix)
                                boat.DE = new DifferentialEvolution(Main.C2SLegs, wind, water,boat,new InterimFix(oldDE.getInitialFix()),new InterimFix(oldDE.getLastFix()),newResolution,Main.searchTolerance,1234,boat.polar.VMG);
                            else
                                boat.DE = new DifferentialEvolution(Main.C2SLegs, wind, water,boat,new Depart(oldDE.getInitialFix()),new InterimFix(oldDE.getLastFix()),newResolution,Main.searchTolerance,1234,boat.polar.VMG);
                            boat.DE.generateAgents();
                            UpdateGraphics();
                            repaint();
                            break;
                        case 19: // CTRL-s
                            SimpleDateFormat formatter= new SimpleDateFormat("yyyyMMdd_HHmmss");
                            Date date = new Date(System.currentTimeMillis());
                            File imageFile = new File(Main.root+"screenshot_"+formatter.format(date)+".png");
                            System.out.println("Saving screenshot: "+imageFile.getName());
                            try {
                                ImageIO.write(image,"png",imageFile);
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                            break;
                        case KeyEvent.VK_CLOSE_BRACKET:
                            cMap.scaleLevel += 1;
                            if (cMap.scaleLevel > 7) cMap.scaleLevel = 7;
                            cMap.update(screen);
                            UpdateGraphics();
                            repaint();
                            break;
                        case KeyEvent.VK_OPEN_BRACKET:
                            cMap.scaleLevel -= 1;
                            if (cMap.scaleLevel < -1) cMap.scaleLevel = -1;
                            cMap.update(screen);
                            UpdateGraphics();
                            repaint();
                            break;
                    }
                    if (e.getKeyCode() == KeyEvent.VK_Z && ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)) {
                        if( undoList.size() > 0) {
                            if (boat.DE != null) {
                                boat.DE.Recycle();
                            }
                            boat = undoList.remove(undoList.size() - 1);
                            UTC.setTimeInMillis(boat.DE.getTime());
                            UpdateGraphics();
                            repaint();
                        }
                    }
                    if(e.getKeyChar() < keyPressed.length){
                        keyPressed[e.getKeyChar()] = false;
//                        System.out.println("key released"+ (int)e.getKeyChar());
                    }
                }

            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    super.mouseMoved(e);
                    if(continuous.running())
                        return;
                    lastX = (int) Math.round(e.getX()*scaling);
                    lastY = (int) Math.round(e.getY()*scaling);
                    Vector2 last = screen.fromPointToLatLng(lastX, lastY).scale(phys.radiansPerDegree);
                    Fix f = new Fix(boat.waypoints[boat.currentWaypoint]);
                    f.heading = new Vector2(Fix.bearing(f.position,last));
                    if (Boat.showConstantCourse) {
                        try {
                            Boat.constantHeading = new LinkedList<>();
                            Boat.constantHeading.add(f);
                            boat.polarToUse = boat.polar.raw;
                            boat.courseToSteer(wind, water, null, last,Boat.constantHeading,phys.msPerHour,(100*phys.rReciprocal),100);
                            boat.polarToUse = boat.polar.VMG;
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (Boat.showConstantTWA) {
                        boat.polarToUse = boat.polar.raw;
                        boat.constantSail(wind, water, Fix.bearing(f.position,last),phys.msPerHour);
                        boat.polarToUse = boat.polar.VMG;
                    }
                    if (Boat.showBestTack) {
                        boat.polarToUse = boat.polar.raw;
                        boat.bestTack(wind, water,last,phys.msPerHour);
                        boat.polarToUse = boat.polar.VMG;
                    }
                    UpdateGraphics();
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    super.mouseDragged(e);
                    endX = (int) Math.round(e.getX()*scaling);
                    endY = (int) Math.round(e.getY()*scaling);
                    int modifiersEx = e.getModifiersEx();
                    int onmask = MouseEvent.SHIFT_DOWN_MASK;
                    if ((modifiersEx & onmask) != onmask) {
                        Vector2 last = screen.fromPointToLatLng(lastX, lastY).scale(phys.radiansPerDegree);
                        Vector2 end = screen.fromPointToLatLng(endX, endY).scale(phys.radiansPerDegree);
                        Vector2 diff = end.minus(last);
                        diff.scaleIP((float) (180 / Math.PI));
                        screen.lon1 -= diff.x;
                        screen.lat1 -= diff.y;
                        screen.lon2 -= diff.x;
                        screen.lat2 -= diff.y;
                        lastX = endX;
                        lastY = endY;
                        screen.computeParameters(0);
                    }
                    cMap.update(screen);
                    UpdateGraphics();
                    repaint();
                }
            });
            addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    super.mousePressed(e);
                    int modifiersEx = e.getModifiersEx();
                    final double mouseX = (int) Math.round(e.getX()*scaling);
                    final double mouseY = (int) Math.round(e.getY()*scaling);
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        Vector2 p = screen.fromPointToLatLng(mouseX, mouseY);
                        ArrayList<TidalStream> clicked = new ArrayList<>();
                        if((modifiersEx & MouseEvent.ALT_DOWN_MASK) == MouseEvent.ALT_DOWN_MASK) {
                            for (TidalStream c : tidalStreams) {
                                if (c.contains(p))
                                    clicked.add(c);
                            }
                            for (TidalStream c : clicked) {
                                boolean toggle = true;
                                for (TidalStream d : clicked) {
                                    if (c == d)
                                        continue;
                                    if (c.contains(d)) {
                                        toggle = false;
                                        break;
                                    }
                                }
                                if (toggle)
                                    c.toggleVisibility();
                            }
                        }
                        else {
                            Chart clickedC = null;
                            float screenScale = Math.abs(screen.lat2 - screen.lat1) * 60 * 1854;
                            for (Chart c : charts) {
                                if (screenScale > 2 * c.scale)
                                    continue;
                                if (screenScale < 0.05 * c.scale)
                                    continue;
                                if (c.contains(p)) {
                                    if (null != clickedC && c.scale > clickedC.scale)
                                        continue;
                                    clickedC = c;
                                }
                            }
                            if (null != clickedC)
                                clickedC.toggleVisibility();
                        }
                        UpdateGraphics();
                        repaint();
                    }
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if((modifiersEx & MouseEvent.ALT_DOWN_MASK) == MouseEvent.ALT_DOWN_MASK) {
                            IDX.IDX_entry closest = idx.closestDrawn(new Vector2(mouseX, mouseY));
                            harmonics.Plot(closest, UTC);
//                        } else if(keyPressed['D']) { // new endpoint
//                            undoList.add(boat);
//                            boat = new Boat(boat);
//                            boat.waypoints[boat.waypoints.length-1].position = screen.fromPointToLatLng(mouseX, mouseY).scale(phys.radiansPerDegree);
//                            newRoute();
//                            boat.DE.PrintInstructions();
//                            UpdateGraphics();
//                            repaint();
//                        } else if(keyPressed['w']) { // next leg
//                            Vector2 p = screen.fromPointToLatLng(mouseX, mouseY).scale(phys.radiansPerDegree);
//                            ModifiedIsochrone.Agent first = modifiedIsochrone.findNearestAgent(boat.DE.getLastFix().position);
//                            ModifiedIsochrone.Agent last = modifiedIsochrone.findNearestAgent(p);
//                            if (ModifiedIsochrone.OutOfOrder(first,last)) {
//                                System.out.println("""
//                                        You have clicked on a point that is before the first DE waypoint.
//                                        Not performing end point operation.
//                                        """);
//                                return;
//                            }
//                            if (ModifiedIsochrone.GetTrackCount(first,last) < 10) {
//                                System.out.println("""
//                                        Your track is too short to fit a DE solution.
//                                        Not performing end point operation.
//                                        """);
//                                return;
//                            }
//
//                            DifferentialEvolution oldDE = boat.DE;
//                            undoList.add(boat);
//                            boat = new Boat(boat);
//                            try {
//                                boat.DE = new DifferentialEvolution(8, first, last, wind, tideGrid, boat, Main.searchTolerance, 1234, boat.polar.raw);
//                                UTC.setTimeInMillis(boat.DE.getTime());
//                            } catch (Exception exception) {
//                                System.out.println("""
//                                        You pressed the w key and left clicked the mouse to extend your DE routing.
//                                        I tried 100000 times but could not find a valid route using MIM as a guide.
//                                        This is normally because there are a lot of islands, or because you are
//                                        trying to route around a headland.
//                                        I will only show the MIM track.
//                                        Try pressing ctrl-z, to go back, and then extending again over a shorter track.
//                                        Also try extending the route in two or more steps; until you are in clear water.
//                                        """);
//                                boat.DE = null;
//                            }
//                            UpdateGraphics();
//                            repaint();
                        } else if(keyPressed['d']) { // new JIM endpoint
                            Vector2 p = screen.fromPointToLatLng(mouseX, mouseY).scale(phys.radiansPerDegree);
                            Agent first;
                            if(boat.DE != null)
                                first = jim.keyAgent.findNearestAgent(boat.DE.departPosition());
                            else
                                first = jim.keyAgent.findNearestAgent(boat.waypoints[0].position);
                            Agent last = jim.keyAgent.findNearestAgent(p);
                            if (JIM.OutOfOrder(first,last)) {
                                System.out.println("""
                                        You have clicked on a point that is before the first DE waypoint.
                                        Not performing end point operation.
                                        """);
                                return;
                            }
                            if (JIM.GetTrackCount(first,last) < 10) {
                                System.out.println("""
                                        Your track is too short to fit a DE solution.
                                        Not performing end point operation.
                                        """);
                                return;
                            }
                            DifferentialEvolution oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
                            if(first.previousAgent == null)
                                boat.DE = new DifferentialEvolution(Main.C2SLegs,last.time,jim,wind, water, boat,  Main.searchTolerance, 1234, boat.polar.VMG);
                            else
                                boat.DE = new DifferentialEvolution(Main.C2SLegs, first, last, wind, water, boat, Main.searchTolerance, 1234, boat.polar.VMG);
                            UTC.setTimeInMillis(boat.DE.getTime());
                            UpdateGraphics();
                            repaint();
                        } else if(keyPressed[3]) { // CTRL-c
                            Fix f = new Fix();
                            f.position = screen.fromPointToLatLng(mouseX, mouseY).scale(phys.radiansPerDegree);
                            String s = f.DMSLatitude()+" "+f.DMSLongitude();
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(new StringSelection(s), null);
                        } else if(keyPressed[4]) { // CTRL-d
                            Vector2 p = screen.fromPointToLatLng(mouseX, mouseY).scale(phys.radiansPerDegree);
                            Fix f = boat.DE.findNearestFix(p);
                            long newResolution = (f.time - boat.DE.getInitialFix().time) / Main.numberOfFixes;
                            DifferentialEvolution oldDE = boat.DE;
                            undoList.add(boat);
                            boat = new Boat(boat);
                            boat.DE = new DifferentialEvolution(oldDE.getLegs(),wind, water,boat,
                                    new InterimFix(oldDE.bestAgent.track.get(0)),new InterimFix(f),newResolution,Main.searchTolerance,1234,boat.polarToUse);
                            boat.DE.generateAgents();
                            boat.DE.PrintInstructions();
                            UTC.setTimeInMillis(boat.DE.getTime() );
                            UpdateGraphics();
                            repaint();
                        } else if(keyPressed['s']) { // new JIM start point
                            if(boat.DE == null)
                                return; // No DE active
                            Vector2 p = screen.fromPointToLatLng(mouseX, mouseY).scale(phys.radiansPerDegree);
                            Agent first = jim.keyAgent.findNearestAgent(p);
                            Agent last = jim.keyAgent.findNearestAgent(boat.DE.bestAgent.waypoint[boat.DE.bestAgent.waypoint.length-1]);

                            if (JIM.OutOfOrder(first,last)) {
                                System.out.println("""
                                        You have clicked on a point that is before the first DE waypoint.
                                        Not performing end point operation.
                                        """);
                                return;
                            }
                            if (JIM.GetTrackCount(first,last) < 10) {
                                System.out.println("""
                                        Your track is too short to fit a DE solution.
                                        Not performing end point operation.
                                        """);
                                return;
                            }
                            undoList.add(boat);
                            boat = new Boat(boat);
                            if(first.previousAgent == null)
                                boat.DE = new DifferentialEvolution(Main.C2SLegs,last.time,jim,wind, water, boat,  Main.searchTolerance, 1234, boat.polar.VMG);
                            else
                                boat.DE = new DifferentialEvolution(Main.C2SLegs, first, last, wind, water, boat, Main.searchTolerance, 1234, boat.polar.VMG);
                            UTC.setTimeInMillis(boat.DE.getTime());
                            UpdateGraphics();
                            repaint();
                        } else {
                            startX = (int) mouseX;
                            startY = (int) mouseY;
                            UpdateGraphics();
                            repaint();
                        }
                    }
                }
            });
            addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    int modifiersEx = e.getModifiersEx();
                    int onmask = MouseEvent.SHIFT_DOWN_MASK;
                    if ((modifiersEx & onmask) != onmask) {
                        Vector2 p = screen.fromPointToLatLng( Math.round(e.getX()*scaling),  Math.round(e.getY()*scaling));
                        float factor = 1.5f;
                        if (e.getWheelRotation() < 0)
                            factor = 1 / factor;
                        else if (e.getWheelRotation()==0) {
                            return;
                        }
                        float dlon = screen.lon2 - screen.lon1;
                        float dlat = screen.lat2 - screen.lat1;
                        screen.lon1 -= (factor - 1) * (p.x - screen.lon1);
                        screen.lat1 -= (factor - 1) * (p.y - screen.lat1);
                        screen.lon2 = screen.lon1 + dlon * factor;
                        screen.lat2 = screen.lat1 + dlat * factor;
                        screen.computeParameters(0);
                        try {
                            if (Main.ShowRouteResolution)
                                wvs.scale(Main.WVSResolution);
                            else
                                wvs.scale(5*screen.scale);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                        cMap.update(screen);

                    } else {
                        onmask = MouseEvent.CTRL_DOWN_MASK;
                        if (e.getWheelRotation() < 0) {
                            if ((modifiersEx & onmask) != onmask)
                                UTC.add(Calendar.MINUTE, 1);
                            else
                                UTC.add(Calendar.HOUR, 1);
                        } else {
                            if ((modifiersEx & onmask) != onmask)
                                UTC.add(Calendar.MINUTE, -1);
                            else
                                UTC.add(Calendar.HOUR, -1);
                        }
                    }
                    UpdateGraphics();
                    repaint();
                }
            });
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    screen.x2[0] = screen.width = e.getComponent().getWidth();
                    screen.y2[0] = screen.height = e.getComponent().getHeight();
                    screen.computeParameters(0);
                    screen.lat2 = screen.bottomRight.y;
                    screen.enabled = true;
                    image = new BufferedImage(screen.width, screen.height, BufferedImage.TYPE_INT_ARGB);
                    cMap.update(screen);

                    UpdateGraphics();

                    repaint();
                }
            });

            try {
                idx.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            addStreams();
            lastRoute.Recompute(wind, water);

            newRoute();

            continuous = new Continuous(this,null);

            UpdateGraphics();
            revalidate();
        }

        void newRoute() {
            try {
                jim = new CrossTrack(boat,wind, water);
                jim.SearchInit();
                jim.Search(jim.route.currentTime + Main.JIMCutoff);
                int temp = boat.currentWaypoint;
                Obstruction.active = boat.waypoints[1].obstructions;
                boat.currentWaypoint = temp;
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
            if (Main.useDifferentialEvolution)
                boat.DE = new DifferentialEvolution(Main.C2SLegs,((Depart)boat.waypoints[0]).getTime()+Main.C2SSearchPeriod,jim,wind, water, boat,  Main.searchTolerance, 1234, boat.polar.raw);
            try {
                UTC.setTimeInMillis(jim.keyAgent.time);
            } catch (NullPointerException e) {
                System.out.println("""
            Suspect that Main.minLon or Main.maxLon do not contain the route.
            Suggest modifying these values to be in excess of the route boundaries.
            """);
                System.exit(-1);
            }
            jframe.setCursor(Cursor.getDefaultCursor());
        }

        void addStreams() {
            TidalStream.harmonics = harmonics;
            TidalStream.idx = idx;
            tidalStreams.add(new TidalStream(Main.root+"Charts/Tides/channel W.png",
                    new int[]{30, 34, 35, 32, 29, 32, 32, 22, 21, 19, 18, 13, 12},
                    new int[]{31, 787, 1550, 2305, 3066, 3819, 4547, 5300, 6060, 6813, 7556, 8307, 9066},
                    new int[]{975, 973, 975, 979, 979, 979, 979, 968, 967, 969, 964, 962, 961},
                    new int[]{649, 1403, 2165, 2920, 3678, 4434, 5164, 5920, 6674, 7425, 8176, 8921, 9678},
                    50 + 45f / 60f, -7 - 10f / 60f,
                    48 + 20f / 60f, -1 - 30f / 60f,
                    "Dover"));
            tidalStreams.add(new TidalStream(Main.root+"Charts/Tides/Lyme bay.png",
                    new int[]{92, 94, 98, 104, 102, 105, 109, 107, 115, 111, 111, 111, 116},
                    new int[]{342, 1594, 2840, 4094, 5342, 6587, 7831, 9076, 10345, 11586, 12834, 14083, 15333},
                    new int[]{940, 940, 948, 950, 952, 955, 960, 955, 963, 961, 959, 960, 965},
                    new int[]{1207, 2459, 3706, 4958, 6208, 7453, 8694, 9941, 11209, 12453, 13699, 14948, 16198},
                    50 + 58f / 60f, -3 - 45f / 60f,
                    49 + 45f / 60f, -1 - 54f / 60f,
                    "Devonport, England"));
            tidalStreams.add(new TidalStream(Main.root+"Charts/Tides/portland race.png",
                    new int[]{225, 235, 236, 231, 226, 224, 221, 215, 212, 211, 206, 210, 212},
                    new int[]{93, 1028, 1920, 2864, 3772, 4719, 5612, 6538, 7456, 8383, 9281, 10210, 11105},
                    new int[]{1319, 1329, 1333, 1321, 1320, 1314, 1314, 1310, 1307, 1303, 1300, 1304, 1308},
                    new int[]{862, 1806, 2701, 3641, 4550, 5497, 6391, 7318, 8235, 9161, 10058, 10990, 11884},
                    50 + 38.8f / 60f, -2 - 38.8f / 60f,
                    50 + 26f / 60f, -2 - 10.8f / 60f,
                    "Devonport, England"));
            tidalStreams.add(new TidalStream(Main.root+"Charts/Tides/channel E.png",
                    new int[]{80, 84, 79, 75, 80, 78, 76, 87, 86, 84, 91, 80, 73},
                    new int[]{134, 917, 1711, 2501, 3298, 4084, 4845, 5639, 6430, 7218, 8007, 8789, 9577},
                    new int[]{1023, 1027, 1022, 1016, 1021, 1020, 1019, 1027, 1029, 1030, 1036, 1022, 1015},
                    new int[]{756, 1538, 2333, 3127, 3925, 4709, 5467, 6265, 7053, 7838, 8628, 9415, 10201},
                    50 + 58.2f / 60f, -2 - 45f / 60f,
                    49 + 05f / 60f, 1 + 40f / 60f,
                    "Dover"));
            tidalStreams.add(new TidalStream(Main.root+"Charts/Tides/North sea S.png",
                    new int[]{36, 794, 1644, 2408, 3199, 3933, 4699, 5455, 6245, 6995, 7784, 8544, 9328},
                    new int[]{117, 132, 133, 151, 159, 165, 180, 193, 203, 212, 220, 228, 235},
                    new int[]{649, 1403, 2257, 3020, 3812, 4541, 5303, 6067, 6855, 7612, 8395, 9163, 9938},
                    new int[]{1046, 1059, 1061, 1077, 1082, 1087, 1109, 1117, 1128, 1137, 1146, 1151, 1163},
                    55 + 15f / 60f, 0 - 20f / 60f,
                    50 + 40f / 60f, 4 + 40f / 60f,
                    "Dover"));
            tidalStreams.add(new TidalStream(Main.root+"Charts/Tides/North sea NW.png",
                    new int[]{32, 779, 1549, 2304, 3080, 3824, 4595, 5357, 6142, 6884, 7663, 8407, 9180},
                    new int[]{112, 121, 132, 143, 152, 161, 163, 179, 187, 192, 197, 201, 202},
                    new int[]{642, 1399, 2164, 2925, 3696, 4446, 5214, 5976, 6763, 7505, 8283, 9031, 9797},
                    new int[]{1045, 1046, 1063, 1066, 1080, 1083, 1090, 1105, 1112, 1117, 1123, 1124, 1130},
                    59 + 50f / 60f, -4 - 00f / 60f,
                    54 + 07.5f / 60f, 3 + 00f / 60f,
                    "Dover"));
            tidalStreams.add(new TidalStream(Main.root+"Charts/Tides/Scotland SW.png",
                    new int[]{134, 1041, 1973, 2891, 3809, 4728, 5662, 6611, 7533, 8483, 9433, 10397, 11312},
                    new int[]{205, 208, 213, 208, 197, 199, 194, 182, 185, 199, 198, 198, 198},
                    new int[]{878, 1787, 2719, 3641, 4558, 5477, 6409, 7359, 8278, 9233, 10182, 11143, 12061},
                    new int[]{1340, 1344, 1349, 1346, 1334, 1338, 1329, 1318, 1319, 1337, 1336, 1332, 1336},
                    57 + 27f / 60f, -8 - 30f / 60f,
                    54 + 08f / 60f, -4 - 37f / 60f,
                    "Dover"));
            tidalStreams.add(new TidalStream(Main.root+"Charts/Tides/Scotland W.png",
                    new int[]{92, 1012, 1919, 2833, 3752, 4674, 5585, 6500, 7412, 8325, 9236, 10155, 11067},
                    new int[]{212, 213, 218, 214, 219, 223, 227, 228, 227, 232, 230, 234, 236},
                    new int[]{882, 1800, 2709, 3622, 4540, 5460, 6373, 7290, 8203, 9114, 10025, 10942, 11859},
                    new int[]{1347, 1346, 1354, 1350, 1353, 1353, 1359, 1364, 1368, 1368, 1363, 1371, 1373},
                    59 + 33f / 60f, -8 - 29.1f / 60f,
                    56 + 37.5f / 60f, -4 - 37f / 60f,
                    "Dover"));
            tidalStreams.add(new TidalStream(Main.root+"Charts/Tides/Irish sea.png",
                    new int[]{50, 801, 1579, 2335, 3115, 3873, 4651, 5406, 6187, 6940, 7713, 8470, 9244},
                    new int[]{109, 104, 108, 109, 114, 111, 117, 123, 125, 130, 138, 143, 143},
                    new int[]{662, 1408, 2188, 2944, 3728, 4480, 5259, 6018, 6796, 7550, 8322, 9082, 9854},
                    new int[]{1061, 1059, 1063, 1062, 1065, 1066, 1072, 1075, 1076, 1083, 1091, 1094, 1093},
                    54 + 55f / 60f, -7 - 20f / 60f,
                    50 + 30f / 60f, -2 - 40f / 60f,
                    "Dover"));
        }

        BufferedImage image = new BufferedImage(1000, 500, BufferedImage.TYPE_INT_ARGB);

        void UpdateGraphics() {
            Graphics g = image.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0,0,image.getWidth(),image.getHeight());
            Graphics2D g2d = (Graphics2D) g;
            Font font = new Font("Arial", Font.BOLD, Main.fontSize);
            if (!screen.enabled)
                return;

            if (null != depth)
                if (cMap.scaleLevel <0)
                    depth.draw(g2d, screen, this);
            if (showWaves)
                waves.draw(g2d, screen, UTC.getTimeInMillis());
            cMap.draw(g,screen);
            wvs.draw(g2d, screen);
            if(Main.useIceZone)
                Boat.iceZone.draw(g2d,screen);
            for (TidalStream tidalStream : tidalStreams)
                tidalStream.draw(g2d, screen, UTC, this);
            g.setColor(Color.LIGHT_GRAY);
            for (Chart chart : charts)
                chart.draw(g2d, screen, this);
            if(Math.abs(screen.lon2 - screen.lon1)<10)
                idx.draw(g2d, screen);
            if(showWind)
                wind.draw(g2d, screen,UTC.getTimeInMillis());
            if(Main.useWater)
                if(showWater)
                    water.draw(g2d, screen, UTC.getTimeInMillis());

            lastRoute.draw(g2d, screen, UTC.getTimeInMillis());
            if(boat.DE != null) {
                boat.DE.draw(g2d, screen, UTC.getTimeInMillis(),Boat.showCandidates);
//                boat.DE.drawTWA(g2d);
            }
            for (Waypoint w:boat.waypoints) {
                w.Draw(g2d, screen);
            }

            jim.draw(g2d, screen, UTC.getTimeInMillis(),Boat.showCandidates);
//            jim.drawTWA(g2d);

            boat.draw(g2d, screen, UTC.getTimeInMillis());

            g.setColor(Color.black);

            Vector2 lastP = new Vector2(lastX,lastY);

            g.setFont(new Font("Arial", Font.BOLD, Main.fontSize));
            IDX.IDX_entry closest = idx.closestDrawn(lastP);
            if(closest != null)
                if(closest.screenPosition.minus(lastP).mag2() < 100)
                    g2d.drawString(closest.IDX_station_name, closest.screenPosition.x, closest.screenPosition.y);

            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb,Locale.UK);

            Fix screenFix = new Fix();

            screenFix.position  = screen.fromPointToLatLng(lastX, lastY).scale(phys.radiansPerDegree);

            formatter.format("%s %s %s. %.0fT %.2f nm",screenFix.DMSLatitude(), screenFix.DMSLongitude(), format.format(UTC.getTime()),
                    Fix.bearing(boat.waypoints[boat.currentWaypoint].position, screenFix.position),
                    Fix.range(boat.waypoints[boat.currentWaypoint].position, screenFix.position) * phys.NM);
            if (boat.polarToUse == boat.polar.raw)
                sb.append(" RAW");
            else
                sb.append(" VMG");
            g.setFont(font);
            g2d.drawString(sb.toString(), 20, 1 * Main.fontSize);

            if (startX != -1) {
                g2d.drawLine(startX, startY, endX, endY);
                Vector2 start = screen.fromPointToLatLng(startX, startY).scale(phys.radiansPerDegree);
                Vector2 end = screen.fromPointToLatLng(endX, endY).scale(phys.radiansPerDegree);
                sb.setLength(0);
                formatter.format("%.0fT (%.0fT) %.2f nm",Fix.bearing(start, end),Fix.bearing(end, start), Fix.range(start, end) * phys.NM);
                g2d.drawString(sb.toString(), (startX + endX) / 2f, (startY + endY) / 2f);
            }
            Vector2 tide = new Vector2();
            if (null != water) {
                Water.SOURCE source = water.getValue(screenFix.position.scale(phys.degrees), UTC.getTimeInMillis(), tide);
                sb.setLength(0);
                formatter.format("%s: %.1f m/s E %.1f m/s N ", Water.Source(source), tide.x, tide.y);
                g2d.drawString(sb.toString(), 20, 2* Main.fontSize);
            }
            if (null != waves) {
                 float height = waves.getValue(screenFix.position.scale(phys.degrees), UTC.getTimeInMillis());
                sb.setLength(0);
                formatter.format("Waves: %.1f m", height);
                g2d.drawString(sb.toString(), 15 * Main.fontSize, 2 * Main.fontSize);
            }

            Vector2 w = new Vector2();
            Wind.SOURCE source= wind.getValue(screenFix.position.scale(phys.degrees),UTC.getTimeInMillis(), w);
            sb.setLength(0);

            formatter.format("%s: %.1f m/s E %.1f m/s N",Wind.Source(source),w.x,w.y);
            g2d.drawString(sb.toString(), 20, 3* Main.fontSize);
                Vector2 tidalWind = w.minus(tide);
                tidalWind = tidalWind.toBearing();
                sb.setLength(0);
                formatter.format("Tidal Wind: %.1f kts  %.0f degrees",tidalWind.x*phys.knots, tidalWind.y+180);
                g2d.drawString(sb.toString(), 20, 4*Main.fontSize);

            sb.setLength(0);
            g2d.drawString(sb.toString(), 20, 5 * Main.fontSize);

        }
        double scaling = 1.0;
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g;
//            final AffineTransform t = g2.getTransform();
//            scaling = t.getScaleX(); // To overcome windows scaling
//            t.setToScale(1, 1);
//            g2.setTransform(t);

            g.drawImage(image,0,0,null);
        }

        void setUTC(long ms) {
            UTC.setTimeInMillis(ms);
        }

        public  Waypoint[] ParseRoute(String string) {
            String[] lines = string.split("\n");
            LinkedList<Waypoint> waypoints = new LinkedList<>();
            List<String> windList = new LinkedList<>();
            List<String> tideList = new LinkedList<>();
            List<String> currentList = new LinkedList<>();
            List<String> wavesList = new LinkedList<>();
            Boolean virtualRegattaWind = false;


            LinkedList<Obstruction> obstructions = null;
            for (String line: lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("#")) {
                    continue;
                }
                else if (trimmedLine.startsWith("Using Wind:")) {
                    if (virtualRegattaWind) {
                        System.out.println("Cannot use VR Wind and grib files at the same time.");
                        System.exit(1);
                    }
                    String[] temp = trimmedLine.split("Using Wind: ");
                    windList.add(Main.root+"grib"+File.separator+temp[1]);
                }
                else if (trimmedLine.startsWith("Using VR Wind:")) {
                    if (!windList.isEmpty()) {
                        System.out.println("Cannot use VR Wind and grib files at the same time.");
                        System.exit(1);
                    }
                    virtualRegattaWind = true;
                }
                else if (trimmedLine.startsWith("Using Polar:")) {
                    POLAR = trimmedLine.split("Using Polar:")[1];
                }
                else if (trimmedLine.startsWith("Using Tide:")) {
                    String[] temp = trimmedLine.split("Using Tide: ");
                    tideList.add(Main.root+"grib"+File.separator+temp[1]);
                }
                else if (trimmedLine.startsWith("Using Current:")) {
                    String[] temp = trimmedLine.split("Using Current: ");
                    currentList.add(Main.root+"grib"+File.separator+temp[1]);
                }
                else if (trimmedLine.startsWith("Using Waves:")) {
                    String[] temp = trimmedLine.split("Using Waves: ");
                    wavesList.add(Main.root+"grib"+File.separator+temp[1]);
                }
                else if (trimmedLine.startsWith("Depart")) {
                    waypoints.add(new Depart(trimmedLine));
                    obstructions = new LinkedList<>();
                }
                else if (trimmedLine.startsWith("Expand")) {
                    Waypoint previous = waypoints.getLast();
                    waypoints.add( new Expand(trimmedLine,obstructions,previous));
                    obstructions = new LinkedList<>();
                }
                else if (trimmedLine.startsWith("Destination")) {
                    Waypoint previous = waypoints.getLast();
                    waypoints.add( new Destination(trimmedLine,obstructions,previous));
                    obstructions = new LinkedList<>();
                }
                else if (trimmedLine.startsWith("Buoy")) {
                    Waypoint previous = waypoints.getLast();
                    waypoints.add( new Buoy(trimmedLine,obstructions,previous));
                    obstructions = new LinkedList<>();
                }
                else if (trimmedLine.startsWith("⎈")) {
                    waypoints.add(new InterimFix(trimmedLine,obstructions));
                    obstructions = new LinkedList<>();
                }
                else if (trimmedLine.startsWith("Fix")) {
                    waypoints.add(new InterimFix(trimmedLine,obstructions));
                    obstructions = new LinkedList<>();
                }
                else if (trimmedLine.startsWith("Gate")) {
                    Waypoint previous = waypoints.getLast();
                    waypoints.add(new Gate(trimmedLine,obstructions,previous));
                    obstructions = new LinkedList<>();
                }
                else if (trimmedLine.startsWith("Diode")) {
                    Waypoint previous = waypoints.getLast();
                    Diode diode = new Diode(trimmedLine,obstructions,previous);
                    waypoints.add(diode);
                    obstructions = new LinkedList<>();
                }
                else if (trimmedLine.startsWith("Leg")) {
                    Waypoint previous = waypoints.getLast();
                    Leg leg = new Leg(trimmedLine,obstructions,previous);
                    waypoints.add(leg);
                    obstructions = new LinkedList<>();
                }
                else if (trimmedLine.startsWith("Obstruction")) {
                    obstructions.add(new Obstruction(trimmedLine));
                }
                else {
                    System.out.println("Waypoint not recognised: "+trimmedLine);
                    System.exit(-1);
                }
            }
            if (!(waypoints.getFirst() instanceof Depart)) {
                System.out.println("first waypoint is not a departure waypoint");
                System.exit(-1);
            }
            UTC.setTimeInMillis(((Depart)waypoints.getFirst()).getTime());

            System.out.println("Don't worry about 'No SLF4J providers were found.' Just an internal warning...");

            wind = new Prevailing(((Depart) waypoints.getFirst()).time);
            if (!windList.isEmpty())
                wind = new SailDocs(windList,wind);
            else
                if (virtualRegattaWind) {
                    VRWind.init();
                    wind = new VRWind(UTC,wind);
                }

            water = new PrevailingCurrent(((Depart) waypoints.getFirst()).time);
            if (!currentList.isEmpty())
                water = new Current(currentList,water);
            if (!tideList.isEmpty())
                water = new Tide(tideList,water);

            if (!wavesList.isEmpty())
                waves = new Waves(wavesList);
            else
                waves = new Waves(((Depart) waypoints.getFirst()).time); // no waves
            return waypoints.toArray(new Waypoint[0]);
        }
    }

    private void parse(StreamPanel s) {
        s.boat.waypoints = s.ParseRoute(Main.ROUTE);

        File userDirectory = new File(Main.root + POLAR);
        if (!userDirectory.exists()) {
            System.out.print("No polars stored for: "+POLAR);
            System.exit(0);
        }
        s.boat.polar = new Polar();
        File[] fileList = userDirectory.listFiles();
        StringBuilder sb = new StringBuilder();
        assert fileList != null;
        System.out.print("\nReading ");
        int i = 0;
        for (File file : fileList) {
            System.out.print(file.getName() + " ("+i+"). ");
            i += 1;
            try {
                sb.setLength(0);
                FileInputStream is = new FileInputStream(file);
                byte[] b = new byte[is.available()];
                int read = is.read(b);
                assert read == b.length;
                sb.append(new String(b));
                sb.append("\n");
            } catch (IOException e) {
                System.out.println("Internal error. Could not read polar " + file.getName());
            }
            s.boat.polar.ScanVirtualRegattaPolar(sb.toString(), file.getName());
        }
        s.boat.polar.combinePolars();
        s.boat.polar.computeVMGPolar();
        System.out.println();

//        System.out.println(s.boat.polar.sail.print());
//        System.out.println(s.boat.polar.printGibeAngles());

        String[] temp = Main.SCREEN.split(" ");
        if (temp.length != 4) {
            System.out.print(
                    "Your input does not follow the format\n" +
                            "51*0'N 35*0'W 0*0'N 0*0'W\n"+
                            "Specifically I was expecting top left and bottom right coordinates.\n");
            System.exit(0);
        }
        float screenTop = Fix.parseLatitude(temp[0]);
        float screenLeft = Fix.parseLongitude(temp[1]);
        float screenRight = Fix.parseLongitude(temp[3]);

        s.screen = new Mercator(screenTop, screenLeft, screenRight);

        File file = new File(Main.root + File.separator + "TidalStreamLog.txt");
        try {
            FileWriter fr = new FileWriter(file, true);
            fr.append(Main.ROUTE+"\n\n");
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void replay(StreamPanel s) {
        s.boat.waypoints = s.ParseRoute(Main.ROUTE);
        File polar = new File(Main.root+POLAR);

        Scanner scanner = new Scanner( Main.REPLAY);

        s.screen = new Mercator(-33f, 133f, 163f);

        UTC.setTimeInMillis(scanner.nextLong()      * phys.msPerSecond);
//TODO
//        Fix depart = new Fix();
//        depart.time = UTC.getTimeInMillis();
//        depart.position = (new Vector2( scanner.nextFloat(), scanner.nextFloat() )).scale(phys.radiansPerDegree);
//        float speed = scanner.nextFloat();
//        depart.heading =  new Vector2(  scanner.nextFloat() );
//        depart.velocity = depart.heading.scale( speed / phys.knots);
//        speed = scanner.nextFloat();
//        depart.wind = new Vector2( scanner.nextFloat());
//        depart.wind = depart.wind.scale( speed  / phys.knots);
//        depart.setSinCos();
//        Fix destination = new Fix();
//
//        destination.position = (new Vector2( scanner.nextFloat(), scanner.nextFloat() )).scale(phys.radiansPerDegree);  // Grenada
//        destination.heading = depart.heading;
//        s.boat.waypoints.add(depart);
//        s.boat.waypoints.add(destination);

        s.boat.polar = new Polar();

        File[] fileList = polar.listFiles();
        StringBuilder sb = new StringBuilder();
        assert fileList != null;
        for (File file : fileList) {
            System.out.println("Reading "+file.getName()+" polar.");
            try {
                sb.setLength(0);
                FileInputStream is = new FileInputStream(file);
                byte[] b = new byte[is.available()];
                int read = is.read(b);
                assert read == b.length;
                sb.append(new String(b));
                sb.append("\n");
            } catch (IOException e) {
                System.out.println("Internal error. Could not read polar " + file.getName());
            }
            s.boat.polar.ScanVirtualRegattaPolar(sb.toString(), file.getName());
        }
        s.boat.polar.combinePolars();
        s.boat.polar.computeVMGPolar();
        System.out.println(s.boat.polar.sail.print());
        System.out.println(s.boat.polar.printGibeAngles());
    }
}