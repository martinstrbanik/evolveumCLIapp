package com.evolveum.cli.command;

import com.evolveum.cli.config.ConfigManager;
import com.evolveum.cli.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.http.HttpResponse;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ModifyUserCommandTest {

    private ModifyUserCommand modifyUserCommand;
    private CommandLine cmd;
    private Properties testConfig;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        modifyUserCommand = new ModifyUserCommand();
        cmd = new CommandLine(modifyUserCommand);

        testConfig = new Properties();
        testConfig.setProperty("url", "http://localhost:8080/midpoint");
        testConfig.setProperty("login", "admin");
        testConfig.setProperty("password", "secret");
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void testModifyUserSuccess() throws Exception {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class);
             MockedConstruction<UserService> mockedService = mockConstruction(UserService.class, (mock, context) -> {
                 HttpResponse<String> mockResponse = mock(HttpResponse.class);
                 when(mockResponse.statusCode()).thenReturn(204);
                 when(mock.modifyUser(anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponse);
             })) {

            mockedConfig.when(() -> ConfigManager.resolveConfigPath(any())).thenReturn("~/.evcliapp.properties");
            mockedConfig.when(() -> ConfigManager.loadConfig(anyString())).thenReturn(testConfig);

            int exitCode = cmd.execute("-o", "00000000-0000-0000-0000-000000000002", "-p", "description", "-v", "new value");

            assertEquals(0, exitCode);
            verify(mockedService.constructed().get(0)).modifyUser("00000000-0000-0000-0000-000000000002", "description", "new value", "replace");
        }
    }

    @Test
    void testModifyUserInvalidType() {
        int exitCode = cmd.execute("-o", "00000000-0000-0000-0000-000000000002", "-p", "description", "-v", "val", "-t", "invalid");
        assertEquals(1, exitCode);
    }

    @Test
    void testModifyUserConflict() throws Exception {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class);
             MockedConstruction<UserService> mockedService = mockConstruction(UserService.class, (mock, context) -> {
                 HttpResponse<String> mockResponse = mock(HttpResponse.class);
                 when(mockResponse.statusCode()).thenReturn(409);
                 when(mock.modifyUser(anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponse);
             })) {

            mockedConfig.when(() -> ConfigManager.resolveConfigPath(any())).thenReturn("~/.evcliapp.properties");
            mockedConfig.when(() -> ConfigManager.loadConfig(anyString())).thenReturn(testConfig);

            int exitCode = cmd.execute("-o", "00000000-0000-0000-0000-000000000002", "-p", "name", "-v", "existing");

            assertEquals(1, exitCode);
        }
    }
}