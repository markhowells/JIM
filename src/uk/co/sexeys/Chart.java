package uk.co.sexeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.sexeys.rendering.Projection;
import uk.co.sexeys.rendering.Renderable;
import uk.co.sexeys.rendering.Renderer;

import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.*;
import java.util.ArrayList;

/**
 * Created by Jim on 15/06/2018.
 *
 * Downloading SHOM charts:
 * FIREFOX
 * goto http://whatismyscreenresolution.net/multi-screen-test
 * enter https://data.shom.fr/ abd press test
 * -->Access to SHOM Catalogue--> Charts
 * Choose raster marine at scale required 150 is a good starting point
 * Find area of interest
 * Scale until all text readable
 * Set limits mormally 5000 by 3000 pixels
 * Move until correct
 * shift-ctrl-k-->:screenshot --fullpage 1.png
 * Downloads-->find file-->right click-->edit
 * Zoom bottom right. Carefully select, zoom out (ctrl-wheel) and rough select top left.
 * Crop
 * Zoom top left. Carefully select, zoom out (ctrl-wheel) and rough select bottom right.
 * Crop
 * Save as PNG
 * Change scale of web page to screen. Hover over page to get coordinates. Edit Charts
 */
public class Chart extends Mercator implements Renderable {
    private static final Logger logger = LoggerFactory.getLogger(Chart.class);
    private final File file;

    BufferedImage image = null;
    float scale;

    void enable() {
        super.enable();
        try {
                image = ImageIO.read(file);
        } catch (Exception e ) {
            logger.error("Read error: {}", e.getMessage());
            System.gc();
            disable();
        } catch (OutOfMemoryError e) {
            logger.error("Out of memory. Could not read image: {}", e.getMessage());
            System.gc();
            disable();
        }
    }

    void disable() {
        super.disable();
        image = null;
    }

    public boolean visibleAtThisScale(float scale) {
        if (scale > 2 * this.scale)
            return false;
        return !(scale < 0.05 * this.scale);
    }
    void draw(Graphics2D g, Mercator screen, ImageObserver i) {
        if (! visibleAtThisScale(screen.scale))
            return;

        Vector2 stl = screen.fromLatLngToPoint(topLeft);
        Vector2 sbr = screen.fromLatLngToPoint(bottomRight);
        if (enabled)
            g.drawImage(image, (int) stl.x, (int) stl.y, (int) sbr.x, (int) sbr.y, 0, 0, image.getWidth(), image.getHeight(), i);
        else {
            g.setColor(Color.magenta);
            g.drawRect((int) stl.x, (int) stl.y, (int) (sbr.x - stl.x), (int) (sbr.y - stl.y));
        }
    }

    @Override
    public void render(Renderer renderer, Projection projection, long time) {
        if (!visibleAtThisScale(projection.getScale()))
            return;

        Vector2 stl = projection.fromLatLngToPoint(topLeft.y, topLeft.x);
        Vector2 sbr = projection.fromLatLngToPoint(bottomRight.y, bottomRight.x);
        if (enabled && image != null) {
            String imageKey = file.getAbsolutePath();
            renderer.drawImage(imageKey, stl.x, stl.y, sbr.x, sbr.y, 0, 0, image.getWidth(), image.getHeight());
        } else {
            renderer.setColor(255, 0, 255); // magenta
            renderer.drawRect(stl.x, stl.y, sbr.x - stl.x, sbr.y - stl.y);
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getImageKey() {
        return file.getAbsolutePath();
    }

    public void computeParameters(int dummy) {
        _pixelsPerLonDegree = (x1[0] - x2[0]) / (lon1 - lon2);
        _pixelsPerLonRadian = _pixelsPerLonDegree * 360 / (2 * Math.PI);
        _pixelOrigin = new Vector2(-x1[0], -y1[0]);
        _pixelOrigin = fromLatLngToPoint(lat1, lon1);
        _pixelOrigin.x *= -1;
        _pixelOrigin.y *= -1;
        topLeft = fromPointToLatLng(new Vector2(0, 0));
        bottomRight = fromPointToLatLng(new Vector2(width, height));
        Vector2 temp = fromPointToLatLng(new Vector2(0, y2[0]));
        if (lat1 > lat2)
            bottomRight.y += (lat2 - temp.y);
        else
            topLeft.y += (lat2 - temp.y);
    }

    static void addTable(String d, ArrayList<Chart> c) {
        try {
            File file = new File(d + "/Charts.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                String name = tokens[0];
                float scale = Float.parseFloat(tokens[1]);
                int topLeftLatitudeDegrees = Integer.parseInt(tokens[2]);
                float topLeftLatitudeMinutes = Float.parseFloat(tokens[3]);
                String topLeftLatitudeSign = tokens[4];
                int topLeftLongitudeDegrees = Integer.parseInt(tokens[5]);
                float topLeftLongitudeMinutes = Float.parseFloat(tokens[6]);
                String topLeftLongitudeSign = tokens[7];
                int bottomRightLatitudeDegrees = Integer.parseInt(tokens[8]);
                float bottomRightLatitudeMinutes = Float.parseFloat(tokens[9]);
                String bottomRightLatitudeSign = tokens[10];
                int bottomRightLongitudeDegrees = Integer.parseInt(tokens[11]);
                float bottomRightLongitudeMinutes = Float.parseFloat(tokens[12]);
                String bottomRightLongitudeSign = tokens[13];
                File f = new File(d + "/" + name + ".png");
                if (!f.exists()) {
                    System.out.println(f.getName() + " does not exist.");
                    continue;
                }
                float topleftLatitude = buildAngle(topLeftLatitudeDegrees,
                        topLeftLatitudeMinutes,
                        topLeftLatitudeSign);
                float topleftLongitude = buildAngle(topLeftLongitudeDegrees,
                        topLeftLongitudeMinutes,
                        topLeftLongitudeSign);
                float bottomRightLatitude = buildAngle(bottomRightLatitudeDegrees,
                        bottomRightLatitudeMinutes,
                        bottomRightLatitudeSign);
                float bottomRightLongitude = buildAngle(bottomRightLongitudeDegrees,
                        bottomRightLongitudeMinutes,
                        bottomRightLongitudeSign);
                int i = 0;
                for (; i < c.size(); i++) {
                    if (scale > c.get(i).scale)
                        break;
                }
                if (i < c.size())
                    c.add(i, new Chart(f, scale, topleftLatitude, topleftLongitude, bottomRightLatitude, bottomRightLongitude));
                else
                    c.add(new Chart(f, scale, topleftLatitude, topleftLongitude, bottomRightLatitude, bottomRightLongitude));
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Chart(File f, float s, float lat1, float lon1, float lat2, float lon2) {
        super(0, 0, lat1, lon1, 1, 1, lat2, lon2);
        file = f;
        width = 1;
        height = 1;
        scale = s;
        computeParameters(0);
    }

}
