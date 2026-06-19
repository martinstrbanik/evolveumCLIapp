package com.evolveum.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "config-init", description = "Initialize Midpoint connection configuration")
public class ConfigInitCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigInitCommand.class);

    @Option(names = {"-u", "--url"}, required = true, description = "Midpoint base URL (e.g. https://demo.evolveum.com/midpoint)")
    private String url;

    @Option(names = {"-l", "--login"}, required = true, description = "Username")
    private String login;

    @Option(names = {"-p", "--password"}, required = true, description = "Password")
    private String password;

    @Override
    public Integer call() {
        try {
            // Simple normalization
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            ConfigManager.saveConfig(url, login, password);
            System.out.println("Configuration successfully saved to ~/.evcliapp.properties");
            return 0;
        } catch (Exception e) {
            logger.error("Failed to save configuration: " + e.getMessage());
            return 1;
        }
    }
}
