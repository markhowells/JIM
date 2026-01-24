package uk.co.sexeys;

import uk.co.sexeys.rendering.Colors;
import uk.co.sexeys.rendering.Projection;
import uk.co.sexeys.rendering.Renderable;
import uk.co.sexeys.rendering.Renderer;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.InterimFix;
import uk.co.sexeys.wind.Wind;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class LastRoute implements Renderable {
    Path path = Paths.get(Main.root + File.separator + "LastRoute.txt");
    public LinkedList<InterimFix> waypoints= new LinkedList<>();
    public LastRoute() {
        try {
            String content = Files.readString(path, Charset.defaultCharset());
            String[] lines = content.split("\n");
            for (String line:lines) {
                String trimmed = line.trim().replaceAll(" +", " ");
                InterimFix w = new InterimFix(trimmed, null);
                waypoints.add(w);
            }
        } catch (IOException | NumberFormatException  | ArrayIndexOutOfBoundsException e) {
            System.out.println("\n database/LastRoute.txt not found, or is corrupt.");
        }

    }
    LastRoute(DifferentialEvolution DE) {
        Fix firstFix = DE.bestAgent.stub;
        Fix lastFix = DE.bestAgent.track.getLast();
        Fix previousFix = firstFix;

        for (Fix f: DE.bestAgent.track) {
            if(f.time == 0)
                continue;
            if (    ( f != firstFix) &&
                    ( f != lastFix) &&
                    (Math.round(f.heading.toBearing().y) == Math.round(previousFix.heading.toBearing().y)) &&
                    ( f.stamina.penaltyEndTime <= 0) ) {
                previousFix = f;
                continue;
            }
            previousFix = f;
            InterimFix intFix = new InterimFix(f);
            waypoints.add(intFix);
        }
    }

    void WriteFile() {
        File file = path.toFile();
        try {
            FileWriter fr = new FileWriter(file, false);
            for (InterimFix f : waypoints) {
                fr.append(f.toString());
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void Recompute(Wind winds, Water tides) {
        InterimFix lastFix = null;
        Vector2 wind = new Vector2();
        Vector2 tide = new Vector2();
        Vector2 tidalWind = new Vector2();
        Vector2 windPerp = new Vector2();
        Vector2 SOG = new Vector2();
        Vector2 positionDegrees  = new Vector2();
        Vector2 logSpeed = new Vector2();
        Vector2 bearing;

        for (InterimFix f : waypoints) {
            if (lastFix == null)   {
                lastFix = f;
                continue;
            }
            positionDegrees.x = f.position.x * phys.degrees;
            positionDegrees.y = f.position.y * phys.degrees;
            winds.getValue( positionDegrees, f.time, wind);
            tides.getValue( positionDegrees, f.time, tide );
            float timeTaken = (float) (f.time - lastFix.time) / phys.msPerSecond;

            f.position.minus( lastFix.position, SOG );
            SOG.x *= (float) Math.cos( f.position.y );
            SOG.scaleIP( (float) phys.R/ timeTaken);
            SOG.minus( tide, logSpeed );
//            System.out.println(SOG.x+" "+SOG.y+" "+tide.x+" "+tide.y);
            bearing = logSpeed.toBearing();
            f.heading = (bearing.y + 360) % 360;
            f.speed = bearing.x;
            // TODO need to subtract tide from speed
            wind.minus(tide, tidalWind);
            f.TWS = tidalWind.normalise();
            SOG.normalise();
            float cosAngle = -tidalWind.dot(SOG);

            if(cosAngle > 1)
                cosAngle = 1;
            if(cosAngle < -1)
                cosAngle  = -1;
            f.TWA = (float) Math.acos(cosAngle);
            f.closeHauled = cosAngle < 0;
            windPerp.x = tidalWind.y;
            windPerp.y = -tidalWind.x;

            f.portTack = SOG.dot(windPerp) < 0;
            lastFix =f;
        }
        WriteFile();
    }

    void draw(Graphics2D g, Mercator screen, long time) {
        Vector2 p, p0;
        Stroke solid = new BasicStroke(3);
        Stroke fine = new BasicStroke(1);

        g.setColor(Color.darkGray);
        g.setStroke(fine);

        if (waypoints.isEmpty())
            return;
        p0 = screen.fromRadiansToPoint(waypoints.getFirst().position);
        for (InterimFix w : waypoints) {
            w.Draw( g,  screen);
            p = screen.fromRadiansToPoint(w.position);
            g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
            p0 = screen.fromRadiansToPoint(w.position);
        }
        InterimFix Y = null, previous = waypoints.getFirst();
        for (InterimFix w:waypoints) {
            if(w.time >= time) {
                Y = w;
                break;
            }
            previous = w;
        }
        if (null != Y) {
            p0 = screen.fromRadiansToPoint(Y.position);
            p = screen.fromRadiansToPoint(previous.position);
            Vector2 dp = p0.minus(p);
            float t = (float) (time-previous.time)/(float)(Y.time - previous.time);
            p0 = p.plus(dp.scale(t));
            g.setStroke(solid);
            g.drawLine((int)p0.x, (int) p0.y-10, (int) p0.x, (int) p0.y+10);
            g.drawLine((int)p0.x-10, (int) p0.y, (int) p0.x+10, (int) p0.y);
            g.setStroke(fine);
        }

    }

    @Override
    public void render(Renderer renderer, Projection projection, long time) {
        Vector2 p, p0;

        renderer.setColor(Colors.DARK_GRAY);
        renderer.setStrokeWidth(1);

        if (waypoints.isEmpty())
            return;
        p0 = projection.fromRadiansToPoint(waypoints.getFirst().position);
        for (InterimFix w : waypoints) {
            w.render(renderer, projection, time);
            p = projection.fromRadiansToPoint(w.position);
            renderer.drawLine(p0.x, p0.y, p.x, p.y);
            p0 = projection.fromRadiansToPoint(w.position);
        }
        InterimFix Y = null, previous = waypoints.getFirst();
        for (InterimFix w : waypoints) {
            if (w.time >= time) {
                Y = w;
                break;
            }
            previous = w;
        }
        if (null != Y) {
            p0 = projection.fromRadiansToPoint(Y.position);
            p = projection.fromRadiansToPoint(previous.position);
            Vector2 dp = p0.minus(p);
            float t = (float) (time - previous.time) / (float) (Y.time - previous.time);
            p0 = p.plus(dp.scale(t));
            renderer.setStrokeWidth(3);
            renderer.drawLine(p0.x, p0.y - 10, p0.x, p0.y + 10);
            renderer.drawLine(p0.x - 10, p0.y, p0.x + 10, p0.y);
            renderer.setStrokeWidth(1);
        }
    }
}
