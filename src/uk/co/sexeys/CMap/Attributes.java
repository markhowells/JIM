package uk.co.sexeys.CMap;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class Attributes {

    private final String[] names;
    private final String[] dataType;

    public Attributes() {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("./database/Charts/Charts/CM93ed2_2009/CM93ATTR.DIC"))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int size = lines.size();
        names = new String[size];
        dataType = new String[size];

        for (String line : lines) {
            String[] fields = line.split("\\|");
            if (fields.length < 3) continue;
            int value = Integer.parseInt(fields[1]);
            names[value] = fields[0];
            dataType[value] = fields[2];
        }
    }

    public void parseBlock(byte[] data) {
        int i = 0;
        while (i < data.length) {
            int v = data[i++] & 0xFF;
            System.out.print(names[v] + " : ");
            switch (dataType[v]) {
                case "aSTRING": {
                    int start = i;
                    while (data[i] != 0) i++;
                    System.out.println(new String(data, start, i - start));
                    i++;
                    break;
                }
                case "aLONG": {
                    long value = ByteBuffer.wrap(data, i, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
                    System.out.println("Long: " + value);
                    i += 4;
                    break;
                }
                case "aBYTE": {
                    System.out.println("Byte: " + (data[i] & 0xFF));
                    i++;
                    break;
                }
                case "aFLOAT": {
                    float f = ByteBuffer.wrap(data, i, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    System.out.println("Float: " + f);
                    i += 4;
                    break;
                }
                case "aCMPLX": {
                    i += 3;
                    int start = i;
                    while (data[i] != 0) i++;
                    System.out.println("aCMPLX: " + new String(data, start, i - start));
                    i++;
                    break;
                }
                case "aLIST": {
                    int n = data[i++] & 0xFF;
                    for (int j = 0; j < n; j++) {
                        System.out.print((data[i++] & 0xFF) + ",");
                    }
                    System.out.println();
                    break;
                }
                case "aWORD10": {
                    int word = ByteBuffer.wrap(data, i, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
                    System.out.println("Word: " + word);
                    i += 2;
                    break;
                }
                default:
                    throw new RuntimeException("Attribute not recognised: " + v + " " + dataType[v]);
            }
        }
    }

    public String getText(byte[] data) {
        int i = 0;
        while (i < data.length) {
            int v = data[i++] & 0xFF;
            switch (dataType[v]) {
                case "aSTRING": {
                    int start = i;
                    while (data[i] != 0) i++;
                    return new String(data, start, i - start);
                }
                case "aCMPLX": {
                    i += 3;
                    int start = i;
                    while (data[i] != 0) i++;
                    return new String(data, start, i - start);
                }
                case "aLONG": i += 4; break;
                case "aBYTE": i += 1; break;
                case "aFLOAT": i += 4; break;
                case "aLIST": {
                    int n = data[i++] & 0xFF;
                    i += n;
                    break;
                }
                case "aWORD10": i += 2; break;
                default:
                    throw new RuntimeException("Attribute not recognised: " + v + " " + dataType[v]);
            }
        }
        return null;
    }

    public List<Float> getFloats(byte[] data) {
        List<Float> floats = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            int v = data[i++] & 0xFF;
            switch (dataType[v]) {
                case "aSTRING":
                    while (data[i] != 0) i++;
                    i++;
                    break;
                case "aLONG": i += 4; break;
                case "aBYTE": i += 1; break;
                case "aFLOAT": {
                    float f = ByteBuffer.wrap(data, i, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    floats.add(f);
                    i += 4;
                    break;
                }
                case "aCMPLX":
                    i += 3;
                    while (data[i] != 0) i++;
                    i++;
                    break;
                case "aLIST": {
                    int n = data[i++] & 0xFF;
                    i += n;
                    break;
                }
                case "aWORD10": i += 2; break;
                default:
                    throw new RuntimeException("Attribute not recognised: " + v + " " + dataType[v]);
            }
        }
        return floats;
    }
}