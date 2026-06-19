package com.evolveum.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.evcliapp.properties";

    public static void saveConfig(String url, String login, String password) throws IOException {
        Properties props = new Properties();
        props.setProperty("url", url);
        props.setProperty("login", login);
        props.setProperty("password", password);

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "evolveumCLIapp parameters");
        }
    }

    public static Properties loadConfig() throws IOException {
        Properties props = new Properties();
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            throw new FileNotFoundException("Configuration not found. Please run 'config-init' first.");
        }
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        }
        return props;
    }
}
