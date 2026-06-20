package com.evolveum.cli.command;

import com.evolveum.cli.client.MidPointClient;
import com.evolveum.cli.config.ConfigManager;
import com.evolveum.cli.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GetUserCommandTest {

    private GetUserCommand getUserCommand;
    private CommandLine cmd;
    private Properties testConfig;

    @BeforeEach
    void setUp() {
        getUserCommand = new GetUserCommand();
        cmd = new CommandLine(getUserCommand);
        
        testConfig = new Properties();
        testConfig.setProperty("url", "http://localhost:8080/midpoint");
        testConfig.setProperty("login", "admin");
        testConfig.setProperty("password", "secret");
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
