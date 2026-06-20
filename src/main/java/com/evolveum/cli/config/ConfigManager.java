package com.evolveum.cli.config;

import com.evolveum.cli.exception.ConfigurationNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

public class ConfigManager {

    public static String resolveConfigPath(String path) {
        if (path == null) {
            return System.getProperty("user.home") + "/.evcliapp.properties";
        }
        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    public static void saveConfig(String configPath, String url, String login, String password) throws IOException {
        Properties props = new Properties();
        props.setProperty("url", url);
        props.setProperty("login", login);
        
        // Simple obfuscation for password
        String obfuscatedPassword = Base64.getEncoder().encodeToString(password.getBytes());
        props.setProperty("password", obfuscatedPassword);

        try (FileOutputStream out = new FileOutputStream(configPath)) {
            props.store(out, "evolveumCLIapp parameters");
        }
    }

    public static Properties loadConfig(String configPath) throws IOException, ConfigurationNotFoundException {
        Properties props = new Properties();
        File file = new File(configPath);
        if (!file.exists()) {
            throw new ConfigurationNotFoundException("Configuration not found at " + configPath + ". Please run 'config-init' first.");
        }
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        }
        
        // De-obfuscate password
        String obfuscatedPassword = props.getProperty("password");
        if (obfuscatedPassword != null) {
            try {
                String plainPassword = new String(Base64.getDecoder().decode(obfuscatedPassword));
                props.setProperty("password", plainPassword);
            } catch (IllegalArgumentException e) {
                // If it's not valid Base64, assume it was saved previously in plain text
            }
        }
        
        return props;
    }
}
