package uk.co.sexeys;

import java.util.Calendar;
// https://en.wikipedia.org/wiki/Sunrise_equation

public class Sun {

    static long riseTime(Calendar date, Vector2 position) {
        double Julian = toJulian(new int[]{date.get(Calendar.YEAR),date.get(Calendar.MONTH)+1,date.get(Calendar.DAY_OF_MONTH)});
        double n = Julian - 2451545.0 + 0.0008;
        double JStar = n - position.x / (2*Math.PI);
        double M = (357.5291 + 0.98560028 * JStar) % 360;
        double C = 1.9148 * Math.sin(Math.toRadians(M)) +
                0.02 * Math.sin(2*Math.toRadians(M)) + 0.0003 * Math.sin(3*Math.toRadians(M));
        double lamda = (M + C + 180+102.9372) % 360;
        double JTransit = 2451545.0 + JStar + 0.0053 * Math.sin(Math.toRadians(M))
                - 0.0069 * Math.sin(Math.toRadians(2*lamda));
        double delta = Math.asin(Math.sin(Math.toRadians(lamda)) * Math.sin(Math.toRadians(23.44)));
        double omega = Math.acos((
                Math.sin(Math.toRadians(-0.83))
                        - Math.sin(position.y)*Math.sin(delta))
                / (Math.cos(position.y)*Math.cos(delta)));
        double JRise = JTransit - omega / (2*Math.PI)  - Julian;
        int riseHour = (int)(JRise*24);
        int riseMinute = (int) Math.round((12+JRise*24 - riseHour)*60);
        Calendar c = (Calendar) date.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.add(Calendar.HOUR_OF_DAY, riseHour);
        c.set(Calendar.MINUTE, 0);
        c.add(Calendar.MINUTE, riseMinute);
        return c.getTimeInMillis();
    }
    static long setTime(Calendar date, Vector2 position) {
        double Julian = toJulian(new int[]{date.get(Calendar.YEAR),date.get(Calendar.MONTH)+1,date.get(Calendar.DAY_OF_MONTH)});
        double n = Julian - 2451545.0 + 0.0008;
        double JStar = n - position.x / (2*Math.PI);
        double M = (357.5291 + 0.98560028 * JStar) % 360;
        double C = 1.9148 * Math.sin(Math.toRadians(M)) +
                0.02 * Math.sin(2*Math.toRadians(M)) + 0.0003 * Math.sin(3*Math.toRadians(M));
        double lamda = (M + C + 180+102.9372) % 360;
        double JTransit = 2451545.0 + JStar + 0.0053 * Math.sin(Math.toRadians(M))
                - 0.0069 * Math.sin(Math.toRadians(2*lamda));
        double delta = Math.asin(Math.sin(Math.toRadians(lamda)) * Math.sin(Math.toRadians(23.44)));
        double omega = Math.acos((
                Math.sin(Math.toRadians(-0.83))
                        - Math.sin(position.y)*Math.sin(delta))
                / (Math.cos(position.y)*Math.cos(delta)));
        double JSet = JTransit + omega / (2*Math.PI)  - Julian;
        int setHour = (int)(JSet*24);
        int setMinute = (int) Math.round((12+JSet*24 - setHour)*60);
        Calendar c = (Calendar) date.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.add(Calendar.HOUR_OF_DAY, setHour);
        c.set(Calendar.MINUTE, 0);
        c.add(Calendar.MINUTE, setMinute);
        return c.getTimeInMillis();
    }
    /**
     * Returns the Julian day number that begins at noon of
     * this day, Positive year signifies A.D., negative year B.C.
     * Remember that the year after 1 B.C. was 1 A.D.
     * <p>
     * ref :
     * Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
     */
    // Gregorian Calendar adopted Oct. 15, 1582 (2299161)
    public static int JGREG = 15 + 31 * (10 + 12 * 1582);
    public static double HALFSECOND = 0.5;

    public static double toJulian(int[] ymd) {
        int year = ymd[0];
        int month = ymd[1]; // jan=1, feb=2,...
        int day = ymd[2];
        int julianYear = year;
        if (year < 0) julianYear++;
        int julianMonth = month;
        if (month > 2) {
            julianMonth++;
        } else {
            julianYear--;
            julianMonth += 13;
        }

        double julian = (java.lang.Math.floor(365.25 * julianYear)
                + java.lang.Math.floor(30.6001 * julianMonth) + day + 1720995.0);
        if (day + 31 * (month + 12 * year) >= JGREG) {
            // change over to Gregorian calendar
            int ja = (int) (0.01 * julianYear);
            julian += 2 - ja + (0.25 * ja);
        }
        return java.lang.Math.floor(julian);
    }

    /**
     * Converts a Julian day to a calendar date
     * ref :
     * Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
     */
    public static int[] fromJulian(double injulian) {
        int jalpha, ja, jb, jc, jd, je, year, month, day;
        double julian = injulian + HALFSECOND / 86400.0;
        ja = (int) julian;
        if (ja >= JGREG) {
            jalpha = (int) (((ja - 1867216) - 0.25) / 36524.25);
            ja = ja + 1 + jalpha - jalpha / 4;
        }

        jb = ja + 1524;
        jc = (int) (6680.0 + ((jb - 2439870) - 122.1) / 365.25);
        jd = 365 * jc + jc / 4;
        je = (int) ((jb - jd) / 30.6001);
        day = jb - jd - (int) (30.6001 * je);
        month = je - 1;
        if (month > 12) month = month - 12;
        year = jc - 4715;
        if (month > 2) year--;
        if (year <= 0) year--;

        return new int[]{year, month, day};
    }

}
