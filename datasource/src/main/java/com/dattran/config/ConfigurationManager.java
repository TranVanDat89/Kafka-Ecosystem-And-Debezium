package com.dattran.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationManager {
    private static final Properties config = new Properties();
    private static ConfigurationManager instance;

    private ConfigurationManager() {
        loadConfiguration();
    }

    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    private void loadConfiguration() {
        // Load từ application.properties
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                config.load(is);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load application.properties");
        }
        // Override với system properties
        config.putAll(System.getProperties());
    }

    public String getString(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(config.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(config.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(config.getProperty(key, String.valueOf(defaultValue)));
    }
}
