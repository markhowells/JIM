package uk.co.sexeys.waypoint;

import uk.co.sexeys.*;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.Formatter;
import java.util.*;


import static java.lang.Math.abs;

public class InterimFix extends Depart{
    public float speed; // knots
    public float heading; // degrees
    public float TWS; // knots
    public float TWA; // degrees pointing to
    public int sail; // number
    public boolean portTack;
    public boolean closeHauled;
    public Stamina stamina = new Stamina();

    public InterimFix() {
        this.obstructions = new Obstruction(null);
    }

    public Vector2 GetHeading() {return new Vector2(heading);}
    public float GetSpeed() {return speed / phys.knots;}

    public Vector2 GetTWA() {return new Vector2(TWS -180);}
    public float GetTWS() {return TWS / phys.knots;}
    public int GetSail() {return sail;}


    public void Draw(Graphics2D g, Mercator screen) {
        Vector2 p = screen.fromRadiansToPoint(position);
        g.drawLine((int) p.x - 10, (int) p.y, (int) p.x + 10, (int) p.y);
        g.drawLine((int) p.x, (int) p.y - 10, (int) p.x, (int) p.y + 10);
        g.drawRect((int) p.x - 5, (int) p.y - 5,10, 10);
        Vector2 h = new Vector2(heading);
        g.drawLine((int) p.x, (int) p.y , (int) (p.x+ 50 * h.x), (int) (p.y - 50*h.y));
        //TODO TWA
        //TODO speed and TWS?
        obstructions.Draw(g,screen);
    }

    public InterimFix(String string, LinkedList<Obstruction> obstructions) {
        String format = """
Fix: 54*32.1'S 12*3.456'W 2019/04/10 10:23 UTC 5.4 knots 123 T 5.6 TWS 100 TWA Sail 4 99 %
or
⎈\t⛵\t\tVendée Globe\t24/11/2024, 15:11:00 UTC\t12186\t100|0.5\t50.1\t20473.6\t9°38'41.13"S 30°15'39.53"W\t160.759\t107.000\t14.11\t53.8\tC0 (Man)\t15.66\t4.58\t0.00@0\t0.00@0\tundefined\tundefined\tundefined\treach light foil hull\tNo\tNo\tT:15:08:00 Actions: TWA=-105.000 @ 24/11/2024, 16:30:00 UTC; HDG=161.000 @ 24/11/2024, 19:00:00 UTC;
""";
        this.obstructions = new Obstruction(obstructions);
        if (string.startsWith("⎈")) {
            String[] temp = string.split("\t");
            String[] temp2 = temp[9].split(" ");
            position = new Vector2(
                    Fix.parseLongitudeDMS(temp2[1]),
                    Fix.parseLatitudeDMS(temp2[0]))
                    .scale(phys.radiansPerDegree);

            String[] timeS = temp[4].split(", ");
            time =Fix.parseTime(timeS[0],timeS[1]).getTimeInMillis();

            speed = Float.parseFloat(temp[15]);
            heading = Float.parseFloat(temp[10]);
            TWS = Float.parseFloat(temp[12]);
            float TWD = Float.parseFloat(temp[13]);
            TWA = TWD - heading;
            if (TWA < -180f) TWA += 360f;
            else if (TWA > 180f) TWA -= 360f;
            float TWAZEZO = Float.parseFloat(temp[11]);
            assert (abs(TWAZEZO - TWA) <0.2);
            String sailS = temp[14];
            if (sailS.contains("C0")) sail = 0;
            else if (sailS.contains("HG")) sail = 1;
            else if (sailS.contains("Jib")) sail = 2;
            else if (sailS.contains("LG")) sail = 3;
            else if (sailS.contains("LJ")) sail = 4;
            else if (sailS.contains("Spi")) sail = 5;
            else if (sailS.contains("Stay")) sail = 6;
            else assert (sailS.equals("C0 Jib HG LG LJ Spi Stay"));
            portTack = TWA < 0;
            closeHauled = abs(TWA) < 90;
            temp2 = temp[6].split("\\|");
            stamina = new Stamina(Float.parseFloat(temp2[0])/100.0f );
        }
        else {
            String[] temp = string.split("Fix: ");
            String[] temp1 = temp[1].split("\n");
            String[] temp2 = temp1[0].split(" ");
            if (temp2.length != 17) {
                System.out.print(
                        "Your input does not follow the format\n" + format +
                                "Specifically I was expecting seventeen space characters.\n" +
                                "This could be a fault with database/LastRoute.txt. Try deleting this file.");
                System.exit(0);
            }
            if (!temp2[4].equals("UTC")) {
                System.out.print(
                        "Your input does not follow the format\n" + format +
                                "Specifically I can only work with times in UTC\n");
                System.exit(0);
            }
            if (!temp2[6].equals("knots")) {
                System.out.print(
                        "Your input does not follow the format\n" + format +
                                "Specifically I can only work with boat speeds in knots\n");
                System.exit(0);
            }
            if (!temp2[8].equals("T")) {
                System.out.print(
                        "Your input does not follow the format\n" + format +
                                "Specifically I can only work with headings in degrees true\n" );
                System.exit(0);
            }
            if (!temp2[10].equals("TWS")) {
                System.out.print(
                        "Your input does not follow the format\n" + format +
                                "Specifically I can only work with true wind speeds (TWS)\n" );
                System.exit(0);
            }
            if (!temp2[12].equals("TWA")) {
                System.out.print(
                        "Your input does not follow the format\n" + format +
                                "Specifically I can only work with true wind angles (TWA)\n" );
                System.exit(0);
            }

            if (!temp2[13].equals("Sail")) {
                System.out.print(
                        "Your input does not follow the format\n" + format +
                                "Specifically I can only work with sail numbers (Sail)\n" );
                System.exit(0);
            }
            if (!temp2[16].equals("%")) {
                System.out.print(
                        "Your input does not follow the format\n" + format +
                                "Specifically I need you current energy level in percent at the end of the line\n+" +
                                "This could be a fault with database/LastRoute.txt. Try deleting this file."  );
                System.exit(0);
            }
            position = new Vector2(
                    Fix.parseLongitudeDMS(temp2[1]),
                    Fix.parseLatitudeDMS(temp2[0]))
                    .scale(phys.radiansPerDegree);

            time = Fix.parseTime(temp2[2], temp2[3]).getTimeInMillis();
            speed = Float.parseFloat(temp2[5]);
            heading = Float.parseFloat(temp2[7]);
            TWS = Float.parseFloat(temp2[9]);
            TWA = Float.parseFloat(temp2[11]);
            sail = Integer.parseInt(temp2[14]);
            stamina = new Stamina(Float.parseFloat(temp2[15]) /100.0f);
            portTack = TWA < 0;
            closeHauled = abs(TWA) < 90;
        }
    }

    public InterimFix(Fix f) {
        this.obstructions = new Obstruction(null);
        position = new Vector2(f.position);
        time=f.time;
        speed = f.velocity.mag()*phys.knots;
        heading = f.heading.toBearing().y;
        TWS = f.wind.mag()*phys.knots;
        portTack = f.portTack;
        closeHauled = f.closeHauled;
        sail = f.sail;
        stamina = new Stamina(f.stamina);
    }

    public String toString() {
//        "Fix: 54*32.1'S 12*3.456'W 2019/04/10 10:23 UTC 5.4 knots 123 T 5.6 TWS 100 TWA Sail 4\n";
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm zzz");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Formatter formatter = new Formatter(sb, Locale.UK);
        Fix f = new Fix(this);
        formatter.format("Fix: %s %s %s %4.1f knots %03.0f T %4.1f TWS %03.0f TWA Sail %s %3.0f %%\n",f.DMSLatitude(), f.DMSLongitude(), format.format(f.time),
                speed*phys.knots, heading,  TWS*phys.knots, TWA*phys.degrees, "-1", stamina.currentStamina*100.0f);
        return sb.toString();
    }
}
