package uk.co.sexeys.CMap;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.lang.Math;

public class CIB {

    public static final double CM93_semimajor_axis_meters = 6378388.0;
    public static final double DEGREE = Math.PI / 180.0;
    public static String path = "./database/Charts/Charts/CM93ed2_2009";

    // scale name, level, lonStep, latStep
    public static final int[][] scales = {
            // Only numeric parts used (index 2 and 3)
            // Python: ["Z",20000000,120,120]
            {20000000,120,120},
            {3000000,60,60},
            {1000000,30,60},
            {200000,12,60},
            {100000,3,60},
            {50000,1,60},
            {20000,1,60},
            {7500,1,60}
    };

    public static final String[] scaleNames =
            {"Z","A","B","C","D","E","F","G"};

    // ----------------------------------------------------

    public String file;
    public List<GeometryObject> pobject_block;

    public double lon_min, lat_min, lon_max, lat_max;
    public double easting_min, northing_min, easting_max, northing_max;

    public int usn_vector_records;
    public int n_vector_record_points;
    public int m_46;
    public int m_4a;
    public int usn_point3d_records;
    public int m_50;
    public int m_54;
    public int usn_point2d_records;
    public int m_5a;
    public int m_5c;
    public int usn_feature_records;
    public int m_60;
    public int m_64;
    public int m_68;
    public int m_6a;
    public int m_6c;
    public int m_nrelated_object_pointers;
    public int m_72;
    public int m_76;
    public int m_78;
    public int m_7c;

    // Transform fields
    public double transform_x_rate;
    public double transform_y_rate;
    public double transform_x_origin;
    public double transform_y_origin;
    public double min_lat;
    public double min_lon;

    // Record counts
    public int m_n_point2d_records;
    public int m_nfeature_records;
    public int m_nvector_records;
    public int m_n_point3d_records;

    // Blocks
    public List<C93Point> p2dpoint_array = new ArrayList<>();
    public List<GeometryDescriptor> edge_vector_descriptor_block = new ArrayList<>();
    public List<GeometryDescriptor> point3d_descriptor_block = new ArrayList<>();

    // ----------------------------------------------------

    public CIB(String filePath, boolean headerOnly) throws Exception {
        this.file = filePath;
        this.pobject_block = new ArrayList<>();

        try (DataInputStream f =
                     new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)))) {

            ReadHeader(f);
            if (headerOnly) return;

            double delta_x = easting_max - easting_min;
            if (delta_x < 0)
                delta_x += CM93_semimajor_axis_meters * 2.0 * Math.PI;

            transform_x_rate = delta_x / 65535.0;
            transform_y_rate = (northing_max - northing_min) / 65535.0;

            transform_x_origin = easting_min;
            transform_y_origin = northing_min;

            min_lat = lat_min;
            min_lon = lon_min;

            m_nfeature_records = usn_feature_records;
            m_n_point2d_records = usn_point2d_records;
            m_nvector_records = usn_vector_records;
            m_n_point3d_records = usn_point3d_records;

            Read_vector_record_table(f);
            Read_3dpoint_table(f);
            Read_2dpoint_table(f);
            Read_feature_record_table(f);
        }
    }

    // ----------------------------------------------------
    // Read header
    // ----------------------------------------------------

    private void ReadHeader(DataInputStream f) throws IOException {
        DecodeTables.readShort(f);       // word0
        DecodeTables.readInt(f);         // int0
        DecodeTables.readInt(f);         // int1

        lon_min = DecodeTables.readDouble(f);
        lat_min = DecodeTables.readDouble(f);
        lon_max = DecodeTables.readDouble(f);
        lat_max = DecodeTables.readDouble(f);

        if (lon_max < lon_min)
            lon_max += 360.0;

        easting_min = DecodeTables.readDouble(f);
        northing_min = DecodeTables.readDouble(f);
        easting_max = DecodeTables.readDouble(f);
        northing_max = DecodeTables.readDouble(f);

        usn_vector_records = DecodeTables.readShort(f);
        n_vector_record_points = DecodeTables.readInt(f);
        m_46 = DecodeTables.readInt(f);
        m_4a = DecodeTables.readInt(f);

        usn_point3d_records = DecodeTables.readShort(f);
        m_50 = DecodeTables.readInt(f);
        m_54 = DecodeTables.readInt(f);

        usn_point2d_records = DecodeTables.readShort(f);
        m_5a = DecodeTables.readShort(f);
        m_5c = DecodeTables.readShort(f);

        usn_feature_records = DecodeTables.readShort(f);
        m_60 = DecodeTables.readInt(f);
        m_64 = DecodeTables.readInt(f);
        m_68 = DecodeTables.readShort(f);
        m_6a = DecodeTables.readShort(f);
        m_6c = DecodeTables.readShort(f);

        m_nrelated_object_pointers = DecodeTables.readInt(f);
        m_72 = DecodeTables.readInt(f);
        m_76 = DecodeTables.readShort(f);
        m_78 = DecodeTables.readInt(f);
        m_7c = DecodeTables.readInt(f);
    }

    // ----------------------------------------------------
    // Static helpers (GetFilePath, GetCellIndex)
    // ----------------------------------------------------

    public static String GetFilePath(double lat, double lng, int scale) {
        String file = GetCellIndex(lat, lng, scale, false);
        String dir = GetCellIndex(lat, lng, scale, true);
        String name = scaleNames[scale];

        return path + "/" + dir + "/" + name + "/" + file + "." + name;
    }

    public static String GetCellIndex(double lat, double lng,
                                      int scale, boolean root) {

        int dIndex = root ? 2: 1;

        double lon1 = (lng + 360.0) * 3.0;
        while (lon1 >= 1080.0) lon1 -= 1080.0;

        int lon2 = (int)Math.floor(lon1 / scales[scale][dIndex]);
        int lon3 = lon2 * scales[scale][dIndex];

        double lat1 = (lat * 3.0) + 270.0 - 30.0;
        int lat2 = (int)Math.floor(lat1 / scales[scale][dIndex]);
        int lat3 = lat2 * scales[scale][dIndex];

        return String.format("%04d%04d", lat3 + 30, lon3);
    }

    // ----------------------------------------------------
    // Read tables
    // ----------------------------------------------------

    private void Read_vector_record_table(DataInputStream f) throws Exception {
        for (int i = 0; i < m_nvector_records; i++) {
            GeometryDescriptor g = new GeometryDescriptor(f, i);
            g.Read2DPoints(f);
            edge_vector_descriptor_block.add(g);
        }
    }

    private void Read_3dpoint_table(DataInputStream f) throws Exception {
        for (int i = 0; i < m_n_point3d_records; i++) {
            GeometryDescriptor g = new GeometryDescriptor(f, i);
            g.Read3DPoints(f);
            point3d_descriptor_block.add(g);
        }
    }

    private void Read_2dpoint_table(DataInputStream f) throws IOException {
        for (int i = 0; i < m_n_point2d_records; i++) {
            int x = DecodeTables.readShort(f);
            int y = DecodeTables.readShort(f);
            p2dpoint_array.add(new C93Point(x, y));
        }
    }

    private void Read_feature_record_table(DataInputStream f) throws Exception {
        for (int i = 0; i < m_nfeature_records; i++) {
            GeometryObject obj = new GeometryObject(f, this);
            pobject_block.add(obj);
        }
    }

    // ----------------------------------------------------
    // Transform point → lat/lon
    // ----------------------------------------------------

    public double[] Transform(C93Point s, double trans_x, double trans_y) {
        double valx = (s.x() * transform_x_rate) + transform_x_origin;
        double valy = (s.y() * transform_y_rate) + transform_y_origin;

        valx -= trans_x;
        valy -= trans_y;

        double lat = (2.0 * Math.atan(Math.exp(valy / CM93_semimajor_axis_meters))
                - Math.PI / 2.0) / DEGREE;

        double lon = valx / (DEGREE * CM93_semimajor_axis_meters);

        return new double[]{lat, lon};
    }

    // ----------------------------------------------------
    // Find higher scale charts
    // ----------------------------------------------------

    public List<String> GetHigherScales() {
        List<String> out = new ArrayList<>();

        Path p = Paths.get(file).getParent();
        if (p == null) return out;

        String scale = p.getFileName().toString();
        Path parent = p.getParent();
        if (parent == null) return out;

        char higher = (char)(scale.charAt(0) + 1);
        Path dir = parent.resolve(String.valueOf(higher));

        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                for (Path f : ds)
                    out.add(f.toString());
            } catch (IOException ignored) {}
        }

        return out;
    }
}
