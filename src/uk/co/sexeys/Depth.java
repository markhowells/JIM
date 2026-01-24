package uk.co.sexeys;

import uk.co.sexeys.rendering.Projection;
import uk.co.sexeys.rendering.Renderable;
import uk.co.sexeys.rendering.Renderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Created by Jim on 16/06/2018.
 *
 */
public class Depth implements Renderable {
    private String file;

    private int pointsPerMinute;

    public Depth(String f, int p) {
        file = f;
        pointsPerMinute = p;
    }

    private byte[] getCell(int lat, int lon) throws IOException {
        if (lon < -180)
            lon += 360;
        if (lon > 180)
            lon -= 360;
        RandomAccessFile is = new RandomAccessFile(file,"r");
        byte lb[] = new byte[360*180*8];
        LongBuffer offsets = ByteBuffer.wrap(lb).asLongBuffer();
        is.read(lb);
        long offset = offsets.get((lat+90) *360+lon+180);
        is.seek(offset);
        int length = is.readInt();
        byte[] sb = new byte[length];
        is.read(sb);
        is.close();
        Inflater inflater = new Inflater();
        inflater.setInput(sb);
        byte[] result = new byte[(60* pointsPerMinute +1)*(60* pointsPerMinute +1)];
        try {
            int resultLength = inflater.inflate(result);
            if(resultLength != result.length)
                throw new DataFormatException("Unexpected uncompressed length lat: "+ lat+ " lon: "+lon);
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        inflater.end();
        return result;
    }

    class CacheEntry {
        BufferedImage image;
        int lat,lon;
    }
    LinkedList<CacheEntry> cache = new LinkedList<>();

    void draw(Graphics2D g, Mercator screen, ImageObserver io) {
        int latS = (int) Math.floor(screen.bottomRight.y);
        int latE = (int) Math.ceil(screen.topLeft.y);
        int lonS = (int) Math.floor(screen.topLeft.x);
        int lonE = (int) Math.ceil(screen.bottomRight.x);
        if( latE - latS > 10 ) return;
        if( lonE - lonS > 10 ) return;
        int stride = 60*pointsPerMinute+1;
        BufferedImage image= new BufferedImage(stride,stride,BufferedImage.TYPE_3BYTE_BGR);
        int[] out = new int[stride*stride];

        byte[] result;
        for (int lat = latS; lat < latE; lat++) {
            for (int lon = lonS; lon < lonE; lon++) {
                CacheEntry hit = null;
                for (int i = 0; i < cache.size(); i++) {
                    CacheEntry c = cache.get(i);
                    if (c.lat == lat && c.lon == lon) {
                        hit = c;
                        cache.remove(i);
                        break;
                    }
                }
                if( null == hit) {
                    hit = new CacheEntry();
                    hit.lat = lat;
                    hit.lon = lon;
                    hit.image= new BufferedImage(stride,stride,BufferedImage.TYPE_3BYTE_BGR);
                    try {
                        result = getCell(lat, lon);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
//                    ShortBuffer sbuf = ByteBuffer.wrap(result).asShortBuffer();
                    byte v;
                    for (int i = 0; i < out.length; i++) {
                        v = result[i];
                        if (v>0)
                            out[i] = 0xf6c96e;
                        else if (v < 0)
                            out[i] = 0xffffff;
                        else
                            out[i] = 0x7fbed8;
                    }
                    hit.image.setRGB(0,0,stride,stride,out,0,stride);
                }
                cache.addFirst(hit);
                if(cache.size() > 30)
                    cache.removeLast();
                Vector2 stl = screen.fromLatLngToPoint(lat+1,lon+1.0/60/4);
                Vector2 sbr = screen.fromLatLngToPoint(lat,lon+1+1.0/60/4);
                g.drawImage(hit.image, (int) stl.x, (int) stl.y, (int) sbr.x, (int) sbr.y, 0,stride, stride, 0,io);
            }
        }
    }

    @Override
    public void render(Renderer renderer, Projection projection, long time) {
        Vector2 topLeft = projection.getTopLeft();
        Vector2 bottomRight = projection.getBottomRight();

        int latS = (int) Math.floor(bottomRight.y);
        int latE = (int) Math.ceil(topLeft.y);
        int lonS = (int) Math.floor(topLeft.x);
        int lonE = (int) Math.ceil(bottomRight.x);
        if (latE - latS > 10) return;
        if (lonE - lonS > 10) return;
        int stride = 60 * pointsPerMinute + 1;
        int[] out = new int[stride * stride];

        byte[] result;
        for (int lat = latS; lat < latE; lat++) {
            for (int lon = lonS; lon < lonE; lon++) {
                CacheEntry hit = null;
                for (int i = 0; i < cache.size(); i++) {
                    CacheEntry c = cache.get(i);
                    if (c.lat == lat && c.lon == lon) {
                        hit = c;
                        cache.remove(i);
                        break;
                    }
                }
                if (null == hit) {
                    hit = new CacheEntry();
                    hit.lat = lat;
                    hit.lon = lon;
                    hit.image = new BufferedImage(stride, stride, BufferedImage.TYPE_3BYTE_BGR);
                    try {
                        result = getCell(lat, lon);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    byte v;
                    for (int i = 0; i < out.length; i++) {
                        v = result[i];
                        if (v > 0)
                            out[i] = 0xf6c96e;
                        else if (v < 0)
                            out[i] = 0xffffff;
                        else
                            out[i] = 0x7fbed8;
                    }
                    hit.image.setRGB(0, 0, stride, stride, out, 0, stride);
                }
                cache.addFirst(hit);
                if (cache.size() > 30)
                    cache.removeLast();

                Vector2 stl = projection.fromLatLngToPoint(lat + 1, lon + 1.0 / 60 / 4);
                Vector2 sbr = projection.fromLatLngToPoint(lat, lon + 1 + 1.0 / 60 / 4);
                // Use SwingRenderer's direct drawImage method for BufferedImage
                if (renderer instanceof uk.co.sexeys.ui.swing.SwingRenderer) {
                    ((uk.co.sexeys.ui.swing.SwingRenderer) renderer).drawImage(
                            hit.image, stl.x, stl.y, sbr.x, sbr.y, 0, stride, stride, 0);
                }
            }
        }
    }

    public BufferedImage getCachedImage(int lat, int lon) {
        for (CacheEntry c : cache) {
            if (c.lat == lat && c.lon == lon) {
                return c.image;
            }
        }
        return null;
    }
    static class SafeArea {
        int minLat;
        int minLon;
        int maxLat;
        int maxLon;
        short safeDepth;
        int stride;
        Boolean [] area;
        Depth depth;
        int pointsPerDegree;

        SafeArea(Depth depth, int minLat, int minLon, int maxLat, int maxLon, short safeDepth) {
            this.depth = depth;
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
            this.safeDepth = safeDepth;
            int nLatCells = maxLat - minLat;
            int nLonCells = maxLon - minLon;
            pointsPerDegree = 60*depth.pointsPerMinute;
            stride = nLonCells*pointsPerDegree;

            area = new Boolean[nLatCells * pointsPerDegree * stride];
            byte[] result = null;
            for (int lat = minLat; lat < maxLat; lat++) {
                for (int lon = minLon; lon < maxLon; lon++) {
                    try {
                        result = depth.getCell(lat, lon);
                    } catch (IOException e) {
                        e.printStackTrace();
                        area = null;
                    }
                    ShortBuffer sbuf = ByteBuffer.wrap(result).asShortBuffer();
                    int v;
                    int k = (lon-minLon) * pointsPerDegree + (lat-minLat)*stride* pointsPerDegree;
                    for (int i = 0; i < pointsPerDegree; i++) {
                        for (int j = 0; j < pointsPerDegree; j++) {
                            v = sbuf.get();
                            area[k++] = v < safeDepth;
                        }
                        sbuf.get();
                        k += nLonCells * pointsPerDegree - pointsPerDegree;
                    }
                }
            }
        }

        Boolean safe( double lon, double lat, boolean outside) {
            if(lon < minLon) return outside;
            if(lon > maxLon) return outside;
            if(lat < minLat) return outside;
            if(lat > maxLat) return outside;
            int y = (int)((lat - minLat )* pointsPerDegree);
            int x = (int)((lon - minLon )* pointsPerDegree);
            return area[x+y*stride];
        }

        void plot( Graphics2D g, Mercator screen, ImageObserver io) {
            int nLatCells = maxLat - minLat;
            int nLonCells = maxLon - minLon;
            BufferedImage image= new BufferedImage(pointsPerDegree*nLonCells,pointsPerDegree*nLatCells,BufferedImage.TYPE_3BYTE_BGR);
            int output[] = new int[nLatCells * pointsPerDegree * stride];
            for (int i = 0; i < output.length; i++) {
                if (!area[i])
                    output[i] = 0xff0000;
                else
                    output[i] = 0xffffff;
            }
            image.setRGB(0,0,pointsPerDegree*nLonCells,pointsPerDegree*nLatCells,output,0,stride);
            Vector2 stl = screen.fromLatLngToPoint(maxLat,minLon);
            Vector2 sbr = screen.fromLatLngToPoint(minLat,maxLon);
            g.drawImage(image, (int) stl.x, (int) stl.y, (int) sbr.x, (int) sbr.y, 0,pointsPerDegree*nLatCells,pointsPerDegree*nLonCells, 0,io);
        }
    }

}
