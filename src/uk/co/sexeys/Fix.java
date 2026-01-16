package uk.co.sexeys;

import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.Depart;
import uk.co.sexeys.waypoint.InterimFix;
import uk.co.sexeys.waypoint.Waypoint;
import uk.co.sexeys.wind.Wind;

import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Jim on 28/10/2018.
 *
 */
public class Fix {
    public long time = 0;
    public Vector2 position;  // radians
    public float cosLatitude;
    public float sinLatitude;
    public Vector2 heading; // normalised
    public boolean portTack,closeHauled;
    public int sail;
    public Vector2 tide; // m/s
    public Vector2 wind; // m/s
    Wind.SOURCE windSource;
    Water.SOURCE waterSource;
    public Vector2 velocity; // m/s
    public Stamina stamina;

    static final LinkedList<Fix> spare = new LinkedList<>();
    static void InitSpares() {
        for (int i = 0; i < 50000; i++) {
            spare.add(new Fix());
        }
    }

    static Fix get() {
        if(spare.isEmpty()){
            System.out.println("adding more fixes");
            for (int i = 0; i < 50000; i++) {
                spare.add(new Fix());
            }
        }
        return spare.removeLast();
    }
    static Fix get(Fix f) {
        if(spare.isEmpty()){
            System.out.println("adding more fixes");
            for (int i = 0; i < 50000; i++) {
                spare.add(new Fix());
            }
        }
        Fix n = spare.removeLast();
        n.time = f.time;
        n.position.copy(f.position);
        n.heading.copy(f.heading);
        n.portTack = f.portTack;
        n.closeHauled = f.closeHauled;
//        n.penaltyToApply = f.penaltyToApply;
        n.tide.copy(f.tide);
        n.wind.copy(f.wind);
        n.velocity.copy(f.velocity);
        n.cosLatitude = f.cosLatitude;
        n.sinLatitude = f.sinLatitude;
        n.windSource = f.windSource;
        n.waterSource = f.waterSource;
        n.sail = f.sail;
        return n;
    }


    static void recycle(LinkedList<Fix> linkedList) {
        spare.addAll(linkedList);
        linkedList.clear();
    }

    static void recycle(LinkedList<Fix> linkedList, Fix stub) {
        while(linkedList.getLast() != stub) {
            spare.add(linkedList.removeLast());
        }
    }

    public Fix() {
        position = new Vector2();
        heading = new Vector2();
        tide = new Vector2();
        wind = new Vector2();
        velocity = new Vector2();
        stamina = new Stamina();
    }

    Fix (Fix f) {
        time = f.time;
        position = new Vector2(f.position);
        heading = new Vector2(f.heading);
        portTack = f.portTack;
        closeHauled = f.closeHauled;
//        penaltyToApply = f.penaltyToApply;
        sail = f.sail;
        tide = new Vector2(f.tide);
        wind = new Vector2(f.wind);
        velocity = new Vector2(f.velocity);
        cosLatitude = f.cosLatitude;
        sinLatitude = f.sinLatitude;
        windSource = f.windSource;
        waterSource = f.waterSource;
        stamina = new Stamina(f.stamina);
    }
    public Fix (Waypoint waypoint) {
        // TODO assume waypoint has no current speed penalty
        position = new Vector2(waypoint.position);
        setSinCos();
        if(waypoint instanceof Depart)
            time = ((Depart) waypoint).getTime();
        heading = new Vector2();
        wind = new Vector2();
        if(waypoint instanceof InterimFix) {
            time = ((InterimFix) waypoint).getTime();
            heading = ((InterimFix) waypoint).GetHeading();
            velocity = heading.scale(((InterimFix) waypoint).GetSpeed());
            wind = ((InterimFix) waypoint).GetTWA();
            wind.scale(((InterimFix) waypoint).GetTWS() / phys.knots);
            portTack = ((InterimFix) waypoint).portTack;
            sail = ((InterimFix) waypoint).GetSail();
            stamina = new Stamina(((InterimFix) waypoint).stamina);
        }
        tide = new Vector2();
        velocity = new Vector2();
        stamina = new Stamina();
    }
    void AddPosition(Vector2 offset) {
        position.y += offset.y * phys.rReciprocal;
        position.x += offset.x * phys.rReciprocal / cosLatitude;
        setSinCos();
    }

    public static float range(Vector2 start, Vector2 end) {
        double phi1 = start.y;
        double phi2 = end.y;
        double dtheta = (end.y - start.y);
        double dlamda = (end.x - start.x);

        double a = Math.sin(dtheta / 2) * Math.sin(dtheta / 2) +
                        Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(dlamda / 2) * Math.sin(dlamda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (phys.R * c); // meters
    }

    public static float bearing(Vector2 start, Vector2 end) {

        double phi1 = start.y ;
        double phi2 = end.y ;
        double lamda = (end.x - start.x) ;

        double a = Math.atan2(
                Math.sin(lamda) * Math.cos(phi2),
                Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(lamda));
        return (float) (a * phys.degrees + 360) % 360;
    }

    void heading (Vector2 end) {
        double phi1 = position.y;
        double phi2 = end.y;
        double lamda = (end.x - position.x);
        heading.x = (float) (Math.sin(lamda) * Math.cos(phi1));
        heading.y = (float) (Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(lamda));
        heading.normalise();
    }

    public static Vector2 destination(Vector2 start,double bearing, double distance) {
        // bearing in Radians
        // distance in m
        double theta1 = start.y;
        double delta = distance/phys.R;
        double deltaTheta = delta * Math.cos(bearing);
        double theta2 = deltaTheta + theta1;
        double deltaPhi = Math.log(Math.tan(theta2/2+Math.PI/4)/Math.tan(theta1/2+Math.PI/4));
        double q =  Math.abs(deltaPhi) > 10e-12 ? deltaTheta / deltaPhi : Math.cos(theta1); // E-W course becomes ill-conditioned with 0/0
        double deltaLamda = delta*Math.sin(bearing)/q;
        double lamda2 = start.x + deltaLamda;
        // check for some daft bugger going past the pole, normalise latitude if so
        if (Math.abs(theta2) > Math.PI/2) theta2 = theta2>0 ? Math.PI-theta2 : -Math.PI-theta2;
        return new Vector2(lamda2,theta2);
    }

    void setSinCos() {
        cosLatitude = (float) Math.cos(position.y);
        sinLatitude = (float) Math.sin(position.y);
    }


    private final static Vector2 positionDegrees = new Vector2();
    Fix nextWorkingFix (final Boat boat, final Wind wind, final Water water, final long resolution)
            throws Exception {
        final long resolutionSecond = resolution / phys.msPerSecond;
        final Fix workingFix = Fix.get();
//        workingFix.penaltyToApply = 0;
        workingFix.heading.x = heading.x;
        workingFix.heading.y = heading.y;

//        workingFix.time = time + resolution + penaltyToApply;
        workingFix.time = time + resolution;
        workingFix.stamina.currentStamina = this.stamina.currentStamina;
        float factor = stamina.speedFactor(workingFix.time, this.time);
        if(Main.useIceZone) {
            factor *= iceZone.SpeedFactor(this.position);
        }
        velocity.x *= factor;
        velocity.y *= factor;
        float offsetX = (tide.x + velocity.x )*resolutionSecond;
        float offsetY = (tide.y + velocity.y )*resolutionSecond;
        float deltaY = offsetY * phys.rReciprocal;

        workingFix.position.x = position.x + offsetX * phys.rReciprocal / cosLatitude;
        workingFix.position.y = position.y + deltaY;
        workingFix.cosLatitude = cosLatitude - deltaY*sinLatitude; // fast cos
        workingFix.sinLatitude = sinLatitude + deltaY*cosLatitude;

        positionDegrees.x = workingFix.position.x * phys.degrees;
        positionDegrees.y = workingFix.position.y * phys.degrees;
        workingFix.windSource = wind.getValue(positionDegrees, workingFix.time, workingFix.wind);
        if(Main.useWater)
            workingFix.waterSource = water.getValue(positionDegrees, workingFix.time, workingFix.tide); //TODO worth 4/17 of the processing time
        float windSpeed = boat.findSpeed(workingFix);
        workingFix.stamina.recover(resolution,windSpeed);
        return workingFix;
    }


    void draw(Graphics2D g, Mercator screen, long t) {
        Font font = new Font("Arial", Font.PLAIN, Main.fontSize);
        g.setFont(font);
        Vector2 p =draw(g,screen);
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar displayTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        displayTime.setTimeInMillis(time);
        String position = format.format(displayTime.getTime());
        long diff = (time - t)/phys.msPerHour;

        g.drawString(position+" "+diff, (int) p.x+5, (int) p.y-5);
    }

    Vector2 draw(Graphics2D g, Mercator screen) {
        Vector2 p = screen.fromRadiansToPoint(position);
        g.drawLine((int) p.x - 10, (int) p.y, (int) p.x + 10, (int) p.y);
        g.drawLine((int) p.x, (int) p.y - 10, (int) p.x, (int) p.y + 10);
        g.drawLine((int) p.x, (int) p.y , (int) (p.x+ 50 * heading.x), (int) (p.y - 50*heading.y));
        return p;
    }
    private final static StringBuilder sb = new StringBuilder();
    private final static Formatter formatter = new Formatter(sb, Locale.UK);

    String DMLatitude() {
        sb.setLength(0);
        String NS = "N";
        float value = position.y;
        if(value < 0) {
            value *= -1;
            NS = "S";
        }
        int valued = (int)(Math.toDegrees(value));
        double valueF = ((Math.toDegrees(value) - valued)*60);
        int valuem = (int) Math.round(valueF);
        formatter.format("%02d*%02d%s" ,valued,valuem, NS);
        return sb.toString();
    }
    public String DMSLatitude() {
        sb.setLength(0);
        String NS = "N";
        float value = position.y;
        if(value < 0) {
            value *= -1;
            NS = "S";
        }
        int valued = (int)(Math.toDegrees(value));
        double valueF = ((Math.toDegrees(value) - valued)*60);
        int valuem = (int) valueF;
        int values = (int) Math.round((valueF - valuem)*60);
        if(values == 60) {
            values = 0;
            valuem++;
        }
        formatter.format("%02d*%02d'%02d\"%s", valued,valuem, values,NS);
        return sb.toString();
    }
    String DMLongitude() {
        sb.setLength(0);
        String NS = "E";
        float value = position.x;
        if(value < 0) {
            value *= -1;
            NS = "W";
        }
        int valued = (int)(Math.toDegrees(value));
        double valueF = ((Math.toDegrees(value) - valued)*60);
        int valuem = (int) Math.round(valueF);
        formatter.format("%03d*%02d%s" ,valued,valuem, NS);
        return sb.toString();
    }
    public String DMSLongitude() {
        sb.setLength(0);
        String NS = "E";
        float value = position.x;
        if (value < 0) {
            value *= -1;
            NS = "W";
        }
        int valued = (int) (Math.toDegrees(value));
        double valueF = ((Math.toDegrees(value) - valued) * 60);
        int valuem = (int) valueF;
        int values = (int) Math.round((valueF - valuem) * 60);
        if(values == 60) {
            values = 0;
            valuem++;
        }
        formatter.format("%03d*%02d'%02d\"%s", valued, valuem, values, NS);
        return sb.toString();
    }
    public String Tack() {
        if (portTack)
            return "Port";
        else
            return "Stbd";
    }

    public String Penalty() {
        long penalty = stamina.penaltyEndTime-time;
        if (penalty < 0)
            penalty = 0;
        return String.format ("%3.0f %% %3d s",stamina.currentStamina * 100,penalty/phys.msPerSecond);
//        if(penaltyToApply == 0)
//            return "";
//        return "+"+penaltyToApply/phys.msPerSecond+"s";
    }

    public static float parseLatitudeDMS(String data) {
        String[] temp1 = data.split("\\*|°");
        if (temp1.length !=2 ) {
                System.out.println(
                        "Your latitude input: " + data + ", does not follow the format:\n" +
                                "54*32'12^S 12*03'02^W\n" +
                                "Specifically I could not find the asterisk '*' or degree (°) in your latitude.\n");
                System.exit(0);
        }
        int latitudeDegrees = Integer.parseInt(temp1[0]);
        String[] temp2 = temp1[1].split("'");
        if (temp2.length !=2 ) {
            System.out.println(
                    "Your latitude input: "+data+", does not follow the format:\n"+
                            "54*32'12^S 12*03'02^W\n"+
                            "Specifically I could not find the quote ' in your latitude.\n");
            System.exit(0);
        }
        float latitudeMinutes = Float.parseFloat(temp2[0]);
        String[] temp3 = temp2[1].split("\"");
        if (temp3.length !=2 ) {
            System.out.println(
                    "Your latitude input: "+data+", does not follow the format:\n"+
                            "54*32'12^S 12*03'02^W\n"+
                            "Specifically I could not find the symbol \" in your latitude.\n");
            System.exit(0);
        }
        float latitudeSeconds = Float.parseFloat(temp3[0]);
        int latitudeSign = 0;
        if(temp3[1].startsWith("N"))
            latitudeSign = 1;
        else if (temp3[1].startsWith("S"))
            latitudeSign = -1;
        else {
            System.out.println(
                    "Your latitude input: "+data+", does not follow the format:\n"+
                            "54*32'12^S 12*03'02^W\n"+
                            "Specifically I could not find 'N' or 'S' ' in your latitude.\n");
            System.exit(0);
        }
        return latitudeSign * (latitudeDegrees + latitudeMinutes/60f + latitudeSeconds/3600);
    }

    static float parseLatitude(String data) {
        String[] temp1 = data.split("\\*");
        if (temp1.length !=2 ) {
            System.out.println(
                    "Your latitude input: "+data+", does not follow the format:\n"+
                            "54*32.12'S 12*03'W\n"+
                            "Specifically I could not find the asterisk '*' in your latitude.\n");
            System.exit(0);
        }
        int latitudeDegrees = Integer.parseInt(temp1[0]);
        String[] temp2 = temp1[1].split("'");
        if (temp2.length !=2 ) {
            System.out.println(
                    "Your latitude input: "+data+", does not follow the format:\n"+
                            "54*32.12'S 12*03'W\n"+
                            "Specifically I could not find the quote ' in your latitude.\n");
            System.exit(0);
        }
        float latitudeMinutes = Float.parseFloat(temp2[0]);
        int latitudeSign = 0;
        if(temp2[1].startsWith("N"))
            latitudeSign = 1;
        else if (temp2[1].startsWith("S"))
            latitudeSign = -1;
        else {
            System.out.println(
                    "Your latitude input: "+data+", does not follow the format:\n"+
                            "54*32.12'S 12*03'W\n"+
                            "Specifically I could not find 'N' or 'S' ' in your latitude.\n");
            System.exit(0);
        }
        return latitudeSign * (latitudeDegrees + latitudeMinutes/60f);
    }

    public static float parseLongitudeDMS(String data) {
        String[] temp1 = data.split("\\*|°");
        if (temp1.length !=2 ) {
                System.out.println(
                        "Your longitude input: " + data + ", does not follow the format:\n" +
                                "54*32'12^S 12*03'02\"W\n" +
                                "Specifically I could not find the asterisk (*) or degree (°) in your longitude.\n");
                System.exit(0);
        }
        int longitudeDegrees = Integer.parseInt(temp1[0]);
        String[] temp2 = temp1[1].split("'");
        if (temp2.length !=2 ) {
            System.out.println(
                    "Your longitude input: "+data+", does not follow the format:\n"+
                            "54*32'12^S 12*03'02\"W\n"+
                            "Specifically I could not find the quote (') in your longitude.\n");
            System.exit(0);
        }
        float longitudeMinutes = Float.parseFloat(temp2[0]);
        String[] temp3 = temp2[1].split("\"");
        if (temp3.length !=2 ) {
            System.out.println(
                    "Your longitude input: "+data+", does not follow the format:\n"+
                            "54*32'12^S 12*03'02\"W\n"+
                            "Specifically I could not find the symbol \" in your longitude.\n");
            System.exit(0);
        }
        float longitudeSeconds = Float.parseFloat(temp3[0]);
        int longitudeSign = 0;
        if(temp3[1].startsWith("E"))
            longitudeSign = 1;
        else if (temp3[1].startsWith("W"))
            longitudeSign = -1;
        else {
            System.out.println(
                    "Your WVS input: "+data+", does not follow the format:\n"+
                            "54*32'12^S 12*03'02\"W\n"+
                            "Specifically I could not find 'E' or 'W' ' in your longitude.\n");
            System.exit(0);
        }
        return longitudeSign * ( longitudeDegrees +  longitudeMinutes/60f + longitudeSeconds/3600f);
    }


    static float parseLongitude(String data) {
        String[] temp1 = data.split("\\*");
        if (temp1.length !=2 ) {
            System.out.println(
                    "Your longitude input: "+data+", does not follow the format:\n"+
                            "54*32.12'S 12*03'W\n"+
                            "Specifically I could not find the asterisk (*) in your longitude.\n");
            System.exit(0);
        }
        int longitudeDegrees = Integer.parseInt(temp1[0]);
        String[] temp2 = temp1[1].split("'");
        if (temp2.length !=2 ) {
            System.out.println(
                    "Your longitude input: "+data+", does not follow the format:\n"+
                            "54*32.12'S 12*03'W\n"+
                            "Specifically I could not find the quote (') in your longitude.\n");
            System.exit(0);
        }
        float longitudeMinutes = Float.parseFloat(temp2[0]);

        int longitudeSign = 0;
        if(temp2[1].startsWith("E"))
            longitudeSign = 1;
        else if (temp2[1].startsWith("W"))
            longitudeSign = -1;
        else {
            System.out.println(
                    "Your WVS input: "+data+", does not follow the format:\n"+
                            "54*32.12'S 12*03'W\n"+
                            "Specifically I could not find 'E' or 'W' ' in your longitude.\n");
            System.exit(0);
        }
        return longitudeSign * ( longitudeDegrees +  longitudeMinutes/60f);
    }

    public static Calendar parseTime(String date, String time) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        try {
            cal.setTime(sdf.parse(date+" "+time));// all done
        } catch (ParseException e) {
            System.out.println("Could not pare time and daqte in: "+ date+" "+time);
            System.exit(-1);
        }
        return cal;
    }

    public static final IceZone iceZone = new IceZone();

    public void getPenalty(Fix previousFix) {
        stamina.penaltyEndTime = previousFix.stamina.penaltyEndTime;
        if((portTack != previousFix.portTack)) {
            if (previousFix.closeHauled)
                stamina.tackPenalty(wind, time);
            else
                stamina.gybePenalty(wind, time);
        }
        if((sail !=previousFix.sail)) {
            stamina.sailPenalty(wind, time);
        }
    }

}

// http://geomalgorithms.com/a03-_inclusion.html
