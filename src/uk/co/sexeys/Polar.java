package uk.co.sexeys;


import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;


public class Polar {
    public ArrayList<Record> polars = new ArrayList<>();
    public BiLinear raw;
    public BiLinear VMG;
    Coefficient tackAngle;
    Coefficient gibeAngle;
    public SailConfiguration sail = new SailConfiguration();

    public static class Record {
        BiLinear data;
        public String name;

        Record(String name) {
            this.name = name;
            data = new BiLinear();
        }
    }
    public class SailConfiguration {
        int[][] configuration;
        float[] xp,yp;
        int XIndex = 0;
        int YIndex = 0;

        public int get(float x, float y) throws Exception {
            BiLinear data = polars.get(0).data;
            try {
                if (x < data.xp[XIndex])
                    while (x < data.xp[XIndex])
                        XIndex--;
                else if (x > data.xp[XIndex + 1])
                    while (x > data.xp[XIndex + 1])
                        XIndex++;
                if (y < data.yp[YIndex])
                    while (y < data.yp[YIndex])
                        YIndex--;
                else if (y > data.yp[YIndex + 1])
                    while (y > data.yp[YIndex + 1])
                        YIndex++;
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new Exception("Array out of bounds");
            }

            float fx = (x - xp[XIndex]) / (xp[XIndex + 1] - xp[XIndex]);
            float fy = (y - yp[YIndex]) / (yp[YIndex + 1] - yp[YIndex]);

            float vx = fx + XIndex;
            float vy = fy + YIndex;

            return configuration[Math.round(vx)][Math.round(vy)];
        }

        public String print() {
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.UK);
            int maxPolarNameLength = 0;
            for (Record r : polars)
                if (r.name.length() > maxPolarNameLength)
                    maxPolarNameLength = r.name.length();
            if (maxPolarNameLength < 6)
                maxPolarNameLength = 6;
            String floatFormat = "%" + (maxPolarNameLength) + ".1f, ";
            String TWAFormat = "%3.0f*   ";
            String stringFormat = "%" + (maxPolarNameLength) + "s, ";
            sb.append("\n\nTWA\\TWS");
            for (float f : raw.xp)
                formatter.format(floatFormat, f * phys.knots);
            sb.append("\n");
            for (int i = raw.yp.length - 1; i >= 0; i--) {
                formatter.format(TWAFormat, Math.acos(raw.yp[i]) * phys.degrees);
                for (int j = 0; j < raw.xp.length; j++)
                    formatter.format(stringFormat, polars.get(sail.configuration[j][i]).name);
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    void ScanVirtualRegattaPolar(String sPolar, String name) {     //    http://toxcct.free.fr/polars/generator.htm
        Record r = new Record(name);
        String[] lines = sPolar.split("\n");
        String[][] values = new String[lines.length][];
        int l = 0;
        for (String line : lines)
            values[l++] = line.split(";");
        r.data.xp = new float[values[0].length - 1];
        final double[] heading = new double[lines.length - 1];
        double[][] rawData = new double[r.data.xp.length][heading.length];
        int i = 0, j = 0;
        try {
            for (i = 0; i < r.data.xp.length; i++)
                r.data.xp[i] = Float.parseFloat(values[0][i + 1]);
            i = 0;
            for (j = 0; j < heading.length; j++)
                heading[j] = Float.parseFloat(values[j + 1][0]);
            for (i = 1; i < rawData.length + 1; i++) {
                for (j = 1; j < rawData[0].length + 1; j++) {
                    rawData[i - 1][j - 1] = Double.parseDouble(values[j][i]) / phys.knots;
                }
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.out.println("I could not parse one of your '" + name + "' polar numbers. Row " + j + " Column " + i + "\n" +
                    "Rows and columns start from zero.\n" +
                    "If the number is missing then count the number of semicolons in your input data.\n" +
                    "To see your input data send an email to polar@course2steer.co.uk with the subject 'listPolars'." +
                    "The data is tab formatted; sometimes 'Show Original' in your email app gives a better format.");
            for (i = 0; i < values.length; i++) {
                for (j = 0; j < values[i].length; j++) {
                    System.out.print(values[i][j] + "\t");
                }
                System.out.println();
            }
            System.exit(-1);
        }

        if (Main.polarHighWindOnly) {  // Reduce speed to practically nothing for wind speeds below 4 knots
            for (j = 0; j < rawData[0].length; j++) {
                rawData[0][j] = rawData[1][j] = rawData[2][j] = rawData[3][j] = rawData[4][j]= 0.1f;
            }
        }

        for (i = 0; i < r.data.xp.length; i++)
            r.data.xp[i] /= phys.knots;
        r.data.value = new Vector2[r.data.xp.length][heading.length];
        for (i = 0; i < r.data.xp.length; i++) {
            for (j = 0; j < heading.length; j++) {
                r.data.value[i][heading.length - j - 1] = new Vector2( //reversed
                        rawData[i][j] * Math.cos(Math.toRadians(heading[j])),
                        rawData[i][j] * Math.sin(Math.toRadians(heading[j])));
            }
        }
        r.data.yp = new float[heading.length];
        for (i = 0; i < heading.length; i++) { //reversed
            r.data.yp[heading.length - i - 1] = (float) Math.cos(Math.toRadians(heading[i]));
        }

        if (Main.sparsePolar) {
            BiLinear interpolated = new BiLinear();
            interpolated.xp = r.data.xp;
            interpolated.yp = new float[181];
            interpolated.value = new Vector2[interpolated.xp.length][interpolated.yp.length];
            for (int k = 0; k < 181; k++) {
                interpolated.yp[k] = (float) MathUtil.cosd(180-k);
            }
            for (i = 0; i < interpolated.xp.length; i++) {
                for (j = 0; j < interpolated.yp.length; j++) {
                    try {
                        interpolated.value[i][j] =  r.data.vectorInterpolate(interpolated.xp[i],interpolated.yp[j]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            r.data = interpolated;
        }
        polars.add(r);
    }

    void combinePolars() {
        int xLength = 0, yLength = 0;
        for (int index = 0; index < polars.size(); index++) {
            Record p = polars.get(index);
            if (p.data.xp.length != xLength) {
                if (xLength == 0) {
                    xLength = p.data.xp.length;
                    yLength = p.data.yp.length;
                    raw = new BiLinear();
                    raw.value = new Vector2[xLength][yLength];
                    raw.xp = p.data.xp;
                    raw.yp = p.data.yp;
                    sail.configuration = new int[xLength][yLength];
                    sail.xp = p.data.xp;
                    sail.yp = p.data.yp;
                    for (int i = 0; i < xLength; i++) {
                        for (int j = 0; j < yLength; j++) {
                            raw.value[i][j] = p.data.value[i][j];
                            sail.configuration[i][j] = index;
                        }
                    }
                    continue;
                } else {
                    System.out.println("Polar: " + p.name + " has a different number of wind speeds compared to the other polars.");
                    System.exit(-1);
                }
            }
            if (p.data.yp.length != yLength) {
                System.out.println("Polar: " + p.name + " has a different number of True Wind Angles compared to the other polars.");
                System.exit(-1);
            }
            for (int i = 0; i < xLength; i++) {
                for (int j = 0; j < yLength; j++) {
                    if (p.data.value[i][j].mag2() > raw.value[i][j].mag2()) {
                        raw.value[i][j] = p.data.value[i][j];
                        sail.configuration[i][j] = index;
                    }
                }
            }
        }
        for (int i = 0; i < xLength; i++) {  // fix head to wind case where everything = 0
            sail.configuration[i][yLength - 1] = sail.configuration[i][yLength - 2];
        }
    }

    void computeVMGPolar() {
        VMG = new BiLinear();
        VMG.xp = raw.xp;
        VMG.yp = raw.yp;
        VMG.value = new Vector2[VMG.xp.length][VMG.yp.length];

        final double[] tackTWA = new double[raw.xp.length];
        final double[] gibeTWA = new double[raw.xp.length];

//        double[][] data = new double[VMG.xp.length][raw.yp.length];

        for (int i = 0; i < VMG.xp.length; i++) {
            double[] vmg = new double[raw.value[i].length];
            double max = Double.MIN_VALUE;
            int maxIndex = 0;
            double min = Double.MAX_VALUE;
            int minIndex = 0;
            for (int j = 0; j < vmg.length; j++) {
                vmg[j] = raw.value[i][j].x;
                if (vmg[j] > max) {
                    max = vmg[j];
                    maxIndex = j;
                }
                if (vmg[j] < min) {
                    min = vmg[j];
                    minIndex = j;
                }
            }
            tackTWA[i] = raw.yp[maxIndex];
            gibeTWA[i] = raw.yp[minIndex];
            double penalty = 1.0; //50% loss in boat speed for 5 minutes (assume once an hour)
            for (int j = 0; j < vmg.length; j++) {
                if (j > maxIndex)
                    VMG.value[i][j] = new Vector2(
                            penalty * vmg[maxIndex],
                            penalty * vmg[maxIndex] * Math.sqrt(1 / (raw.yp[j] * raw.yp[j]) - 1));
                else if (j < minIndex)
                    VMG.value[i][j] = new Vector2(
                            penalty * vmg[minIndex],
                            -penalty * vmg[minIndex] * Math.sqrt(1 / (raw.yp[j] * raw.yp[j]) - 1));
                else
                    VMG.value[i][j] = raw.value[i][j];
            }
        }

        for (int i = 0; i < gibeTWA.length; i++) {
            if (gibeTWA[i] == 1) {
                if (i == 0)
                    gibeTWA[0] = gibeTWA[1];
                else
                    gibeTWA[i] = gibeTWA[i - 1];
            }
        }
        for (int i = 0; i < tackTWA.length; i++) {
            if (tackTWA[i] == 1) {
                if (i == 0)
                    tackTWA[0] = tackTWA[1];
                else
                    tackTWA[i] = tackTWA[i - 1];
            }
        }
        gibeAngle = new Coefficient(gibeTWA, raw.xp);
        tackAngle = new Coefficient(tackTWA, raw.xp);
    }



    public String printGibeAngles() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.UK);
        int maxPolarNameLength = 4;
        String floatFormat = "%" + (maxPolarNameLength) + ".1f, ";
        String TWAFormat = "%3.0f*, ";
//        String stringFormat = "%" + (maxPolarNameLength) + "s, ";
        sb.append("\n\nTWA\\TWS ");
        for (double f : gibeAngle.parameter)
            formatter.format(floatFormat, f * phys.knots);
        sb.append("\n        ");
        for (int i = 0; i < raw.xp.length; i++) {
            formatter.format(TWAFormat, Math.acos(gibeAngle.value[i]) * phys.degrees);
        }
        return sb.toString();
    }
}