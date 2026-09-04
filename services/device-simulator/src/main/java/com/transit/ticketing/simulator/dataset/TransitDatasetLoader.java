package com.transit.ticketing.simulator.dataset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TransitDatasetLoader {

    public List<TransitRecord> loadFromClasspath(String resourceName) throws IOException {
        InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName);

        if (stream == null) {
            throw new IOException("Classpath resource not found: " + resourceName);
        }

        List<TransitRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                throw new IOException("Dataset is empty: " + resourceName);
            }

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length != 5) {
                    throw new IOException(
                            "Invalid dataset row at line " + lineNumber + ": " + line);
                }

                records.add(new TransitRecord(
                        fields[0].trim(),
                        fields[1].trim(),
                        fields[2].trim(),
                        Integer.parseInt(fields[3].trim()),
                        fields[4].trim()));
            }
        }

        if (records.isEmpty()) {
            throw new IOException("Dataset has no records: " + resourceName);
        }

        return List.copyOf(records);
    }
}
