package com.evolveum.cli.command;

import com.evolveum.cli.config.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConfigInitCommandTest {

    private ConfigInitCommand configInitCommand;
    private CommandLine cmd;

    @BeforeEach
    void setUp() {
        configInitCommand = new ConfigInitCommand();
        cmd = new CommandLine(configInitCommand);
    }

    @Test
    void testConfigInitSuccess() {
        try (MockedStatic<ConfigManager> mockedConfigManager = mockStatic(ConfigManager.class)) {
            // Given
            String url = "https://demo.evolveum.com/midpoint";
            String login = "administrator";
            String password = "password123";

            // When
            int exitCode = cmd.execute("-u", url, "-l", login, "-p", password);

            // Then
            assertEquals(0, exitCode);
            mockedConfigManager.verify(() -> ConfigManager.saveConfig(url, login, password), times(1));
        }
    }

    @Test
    void testConfigInitUrlNormalization() {
        try (MockedStatic<ConfigManager> mockedConfigManager = mockStatic(ConfigManager.class)) {
            // Given: URL with trailing slash
            String urlWithSlash = "https://demo.evolveum.com/midpoint/";
            String expectedUrl = "https://demo.evolveum.com/midpoint";
            String login = "admin";
            String password = "pwd";

            // When
            int exitCode = cmd.execute("-u", urlWithSlash, "-l", login, "-p", password);

            // Then
            assertEquals(0, exitCode);
            mockedConfigManager.verify(() -> ConfigManager.saveConfig(expectedUrl, login, password), times(1));
        }
    }

    @Test
    void testConfigInitMissingRequiredOptions() {
        // When: Running without required -u, -l, -p
        int exitCode = cmd.execute();

        // Then: Picocli should return non-zero exit code for invalid usage
        assertEquals(2, exitCode);
    }

    @Test
    void testConfigInitFailureHandling() {
        try (MockedStatic<ConfigManager> mockedConfigManager = mockStatic(ConfigManager.class)) {
            // Given: ConfigManager throws exception
            mockedConfigManager.when(() -> ConfigManager.saveConfig(anyString(), anyString(), anyString()))
                    .thenThrow(new IOException("Disk full"));

            // When
            int exitCode = cmd.execute("-u", "http://test", "-l", "user", "-p", "pass");

            // Then
            assertEquals(1, exitCode);
        }
    }
}
