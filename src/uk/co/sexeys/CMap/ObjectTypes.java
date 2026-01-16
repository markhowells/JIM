package uk.co.sexeys.CMap;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ObjectTypes {

    private final String[] names;
    private final String[] dataType;

    public ObjectTypes()  {
        String dicFilePath = "./database/Charts/Charts/CM93ed2_2009/CM93OBJ.DIC";
        List<String> lines = new ArrayList<>();

        // Read the file line by line
        try (BufferedReader br = new BufferedReader(new FileReader(dicFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int size = lines.size() * 2; // equivalent to Python: len(lines)*2
        names = new String[size];
        dataType = new String[size];

        for (String line : lines) {
            String[] fields = line.split("\\|");
            if (fields.length < 5) continue; // make sure we have at least 5 fields
            int value = Integer.parseInt(fields[1]);
            names[value] = fields[0];
            dataType[value] = fields[4];
        }
    }
}

