package uk.co.sexeys.waypoint;

import uk.co.sexeys.*;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.TimeZone;

public class Depart extends Waypoint{
//    public Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));;
    public long time;
    public long getTime() { return time;}
    public void ChangeTime(long delta) { time+=delta; }
    public void SetTime(long time) { this.time=time; }

    public boolean Reached(Vector2 dummy, Vector2 dummy1) {
        System.out.println("Something has gone wrong. Tried to reach a Departure Waypoint.");
        System.exit(-1);
        return false;
    }

    public void Draw(Graphics2D g, Mercator screen) {
        Vector2 p = screen.fromRadiansToPoint(position);
        g.drawLine((int) p.x - 10, (int) p.y, (int) p.x + 10, (int) p.y);
        g.drawLine((int) p.x, (int) p.y - 10, (int) p.x, (int) p.y + 10);
        g.drawRect((int) p.x - 5, (int) p.y - 5,10, 10);
    }

    public void Draw(Graphics2D g, Mercator screen, long t) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        Calendar displayTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        displayTime.setTimeInMillis(time);
        String sTime = format.format(displayTime.getTime());
        long diff = (time - t)/phys.msPerHour;

        Vector2 p = screen.fromRadiansToPoint(position);
        g.drawLine((int) p.x - 10, (int) p.y, (int) p.x + 10, (int) p.y);
        g.drawLine((int) p.x, (int) p.y - 10, (int) p.x, (int) p.y + 10);
        g.drawRect((int) p.x - 5, (int) p.y - 5,10, 10);
        Font font = new Font("Arial", Font.PLAIN, 14);
        g.setFont(font);
        g.drawString(sTime+" "+diff, (int) p.x+5, (int) p.y-5);

        obstructions.Draw(g,screen);
    }

    public Depart() {
        this.obstructions = new Obstruction((LinkedList<Obstruction>) null);
    }
    public Depart(String string) {
        String[] temp = string.split("Depart: ");
        String[] temp1 = temp[1].split("\n");
        String[] temp2 = temp1[0].split(" ");
        if (temp2.length != 5) {
            System.out.print(
"""
Your input does not follow the format
Depart: 54*32.1'S 12*3.456'W 2019/04/10 10:23 UTC
Specifically I was expecting five space characters.
Send an email to help@course2steer.co.uk for help.
""");
            System.exit(0);
        }
        if (!temp2[4].equals("UTC")) {
            System.out.print(
"""
Your input does not follow the format
Depart: 54*32.1'S 12*3.456'W 2019/04/10 10:23 UTC
Specifically I can only work with times in UTC
Send an email to help@course2steer.co.uk for help.
""");
            System.exit(0);
        }
        position = new Vector2(
                Fix.parseLongitudeDMS(temp2[1]),
                Fix.parseLatitudeDMS(temp2[0]))
                .scale(phys.radiansPerDegree);

        time = Fix.parseTime(temp2[2], temp2[3]).getTimeInMillis();
    }
    public Depart(Fix f) {
        this.obstructions = new Obstruction((LinkedList<Obstruction>) null);
        position = new Vector2(f.position);
        time=f.time;
    }
}
