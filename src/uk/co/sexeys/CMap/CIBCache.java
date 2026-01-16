package uk.co.sexeys.CMap;
import java.io.File;
import java.util.ArrayList;

public class CIBCache {

    private final ArrayList<CIB> cache = new ArrayList<>();
    private final int MAX = 100;

    public CIB getCIB(String file) throws Exception {
        for (int i = cache.size() - 1; i >= 0; i--) {
            CIB c = cache.get(i);
            if (c.file.equals(file))
                return c;
        }
        CIB c = new CIB(file,false);
        cache.add(c);
        while (cache.size() > MAX)
            cache.removeFirst();
        return c;
    }

    public ArrayList<CIB> GetChartFiles(double top, double left,
                                        double bottom, double right,
                                        int scale) throws Exception {
        ArrayList<String> paths = new ArrayList<>();
        double inc = CIB.scales[scale][1]/3.0;
        for (double x = left; x < right+inc  ; x+= inc) {
            for (double y= bottom; y < top+inc; y+= inc) {
                String p = CIB.GetFilePath(y, x, scale);
                if (!paths.contains(p) && new File(p).length() > 0) paths.add(p);
            }
        }


        ArrayList<CIB> list = new ArrayList<>();
        for (String f : paths)
            list.add(getCIB(f));

        return list;
    }
}

