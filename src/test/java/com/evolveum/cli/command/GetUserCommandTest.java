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

class GetUserCommandTest {

    private GetUserCommand getUserCommand;
    private CommandLine cmd;
    private Properties testConfig;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        getUserCommand = new GetUserCommand();
        cmd = new CommandLine(getUserCommand);
        
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
    void testGetUserSuccess() throws Exception {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class);
             MockedConstruction<UserService> mockedService = mockConstruction(UserService.class, (mock, context) -> {
                 HttpResponse<String> mockResponse = mock(HttpResponse.class);
                 when(mockResponse.statusCode()).thenReturn(200);
                 when(mockResponse.body()).thenReturn("{\"name\":\"test-user\"}");
                 when(mock.getUser(anyString())).thenReturn(mockResponse);
             })) {
            
            mockedConfig.when(ConfigManager::loadConfig).thenReturn(testConfig);

            int exitCode = cmd.execute("-o", "00000000-0000-0000-0000-000000000002");

            assertEquals(0, exitCode);
            verify(mockedService.constructed().get(0)).getUser("00000000-0000-0000-0000-000000000002");
            assertTrue(outContent.toString().contains("test-user"));
        }
    }

    @Test
    void testGetUserInvalidOid() {
        int exitCode = cmd.execute("-o", "invalid-oid-format");
        assertEquals(1, exitCode);
    }

    @Test
    void testGetUserNotFound() throws Exception {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class);
             MockedConstruction<UserService> mockedService = mockConstruction(UserService.class, (mock, context) -> {
                 HttpResponse<String> mockResponse = mock(HttpResponse.class);
                 when(mockResponse.statusCode()).thenReturn(404);
                 when(mock.getUser(anyString())).thenReturn(mockResponse);
             })) {
            
            mockedConfig.when(ConfigManager::loadConfig).thenReturn(testConfig);

            int exitCode = cmd.execute("-o", "00000000-0000-0000-0000-000000000002");

            assertEquals(1, exitCode);
        }
    }
}