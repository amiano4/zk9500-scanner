package com.sparkcleancebu.biometrics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;


public class ConfigReader {
    private Map<String, String> config;
    private String filePath;

    @SuppressWarnings("unchecked")
    public ConfigReader(String filePath) {
        this.filePath = filePath;
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filePath);

        try {
            // Ensure parent directories exist
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Create file if it does not exist
            if (!file.exists()) {
                Files.write(file.toPath(), "{}".getBytes(), StandardOpenOption.CREATE);
            }

            // Read and parse the JSON file
            this.config = objectMapper.readValue(file, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
            this.config = new HashMap<>(); // Use an empty map if file read fails
        }
    }

    public String get(String key) {
        return (this.config != null) ? this.config.getOrDefault(key, null) : null;
    }

    public void set(String key, String value) {
        // Update the in-memory map
        if (this.config != null) {
            this.config.put(key, value);
        }

        // Write the updated map to the config.json file
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Write the map as JSON to the file
            objectMapper.writeValue(new File(filePath), this.config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
