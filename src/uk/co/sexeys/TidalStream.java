package uk.co.sexeys;

import uk.co.sexeys.rendering.Projection;
import uk.co.sexeys.rendering.Renderable;
import uk.co.sexeys.rendering.Renderer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;

import static java.lang.System.exit;

/**
 * Created by Jim on 04/06/2018.
 *
 */
public class TidalStream extends Mercator implements Renderable {
    private File file;
    private IDX.IDX_entry idxEntry;
    private BufferedImage image = null;
    static Harmonics harmonics = null;
    static IDX idx = null;

    TidalStream(String f, int[] x1, int[] y1, int x2[], int y2[], float lat1, float lon1, float lat2, float lon2, String station) {
        super( x1, y1,  x2,  y2, lat1, lon1, lat2, lon2);
        idxEntry = idx.FindEntry(station);
        if( idxEntry.pref_sta_data == null ) {
            try {
                idxEntry.pref_sta_data = harmonics.FindStation(idxEntry.IDX_reference_name);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        if(idxEntry.pref_sta_data == null) {
            System.out.println("Could not find station:\n"+idxEntry.IDX_reference_name+"\nin\n"+idxEntry.IDX_station_name);
            System.exit(1);
        }
        file = new File(f);
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(file);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis, true);
                width = reader.getWidth(0);
                height = reader.getHeight(0);
            }
        } catch (IOException e) {
            System.out.println("read error: " + e.getMessage());
        }
        computeParameters(0);

    }

    void enable() {
        super.enable();
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            System.out.println("read error: " + e.getMessage());
        }
    }

    void disable() {
        super.disable();
        image = null;
    }


    /**
     * Draws as much of the specified chart.
     * <p>
     *
     * @param g      the specified <code>Graphics2D</code> in whic h to draw.
     * @param screen the <i>screen</i> on which the tidalStreams will be drawn.
     * @param time   the time.
     * @param io     the image observer (<code>this</code>).
     */
//    void draw(Graphics2D g, Mercator screen, int t, ImageObserver io) {
//  /*          if(topLeft.x > screen.bottomRight.x) return;
//            if(topLeft.y < screen.bottomRight.y) return;
//            if(bottomRight.x < screen.topLeft.x) return;
//            if(bottomRight.y > screen.topLeft.y) return; */
//        int i = (t+timeOffset)%x1.length;
//        Vector2 stl = screen.fromLatLngToPoint(topLeft);
//        Vector2 sbr = screen.fromLatLngToPoint(bottomRight);
//        if (enabled)
//            g.drawImage(image, (int) stl.x, (int) stl.y, (int) sbr.x, (int) sbr.y, x1[i], y1[i], x2[i], y2[i], io);
//      //  else
//          //  g.drawRect((int) stl.x, (int) stl.y, (int) (sbr.x - stl.x), (int) (sbr.y - stl.y));
//    }

    void draw(Graphics2D g, Mercator screen, Calendar time, ImageObserver io) {
  /*          if(topLeft.x > screen.bottomRight.x) return;
            if(topLeft.y < screen.bottomRight.y) return;
            if(bottomRight.x < screen.topLeft.x) return;
            if(bottomRight.y > screen.topLeft.y) return; */
        if (!enabled)
            return;
        Compute compute = new Compute(harmonics,idxEntry,time.get(Calendar.YEAR));
        long startTime = compute.timeFromYearStart(time);
        long nextHW = compute.NextHighWater(startTime) - startTime;

        if(nextHW > 6.5*60*60)
            nextHW = compute.PreviousHighWater(startTime) - startTime;
        int i = (int) (-nextHW/(3600.)+6.5);
        if (i < 0)
            i += x1.length;
        if (i > x1.length)
            i -= x1.length;
        Vector2 stl = screen.fromLatLngToPoint(topLeft);
        Vector2 sbr = screen.fromLatLngToPoint(bottomRight);
        g.drawImage(image, (int) stl.x, (int) stl.y, (int) sbr.x, (int) sbr.y, x1[i], y1[i], x2[i], y2[i], io);
    }

    @Override
    public void render(Renderer renderer, Projection projection, long time) {
        if (!enabled)
            return;

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(time);

        Compute compute = new Compute(harmonics, idxEntry, calendar.get(Calendar.YEAR));
        long startTime = compute.timeFromYearStart(calendar);
        long nextHW = compute.NextHighWater(startTime) - startTime;

        if (nextHW > 6.5 * 60 * 60)
            nextHW = compute.PreviousHighWater(startTime) - startTime;
        int i = (int) (-nextHW / (3600.) + 6.5);
        if (i < 0)
            i += x1.length;
        if (i > x1.length)
            i -= x1.length;

        Vector2 stl = projection.fromLatLngToPoint(topLeft.y, topLeft.x);
        Vector2 sbr = projection.fromLatLngToPoint(bottomRight.y, bottomRight.x);

        String imageKey = file.getAbsolutePath();
        renderer.drawImage(imageKey, stl.x, stl.y, sbr.x, sbr.y, x1[i], y1[i], x2[i], y2[i]);
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getImageKey() {
        return file.getAbsolutePath();
    }
}

