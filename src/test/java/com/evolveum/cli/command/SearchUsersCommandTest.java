package com.evolveum.cli.command;

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

class SearchUsersCommandTest {

    private SearchUsersCommand searchUsersCommand;
    private CommandLine cmd;
    private Properties testConfig;

    @BeforeEach
    void setUp() {
        searchUsersCommand = new SearchUsersCommand();
        cmd = new CommandLine(searchUsersCommand);

        testConfig = new Properties();
        testConfig.setProperty("url", "http://localhost:8080/midpoint");
        testConfig.setProperty("login", "admin");
        testConfig.setProperty("password", "secret");
    }

    @Test
    void testSearchUsersSuccess() throws Exception {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class);
             MockedConstruction<UserService> mockedService = mockConstruction(UserService.class, (mock, context) -> {
                 HttpResponse<String> mockResponse = mock(HttpResponse.class);
                 when(mockResponse.statusCode()).thenReturn(200);
                 // Minimal JSON structure for Jackson parser
                 when(mockResponse.body()).thenReturn("{\"object\":{\"object\":[{\"oid\":\"123\",\"name\":\"peter\"}]}}");
                 when(mock.searchUsers(anyString())).thenReturn(mockResponse);
             })) {

            mockedConfig.when(ConfigManager::loadConfig).thenReturn(testConfig);

            int exitCode = cmd.execute("-q", "peter");

            assertEquals(0, exitCode);
            verify(mockedService.constructed().get(0)).searchUsers("peter");
        }
    }

    @Test
    void testSearchUsersNoResults() throws Exception {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class);
             MockedConstruction<UserService> mockedService = mockConstruction(UserService.class, (mock, context) -> {
                 HttpResponse<String> mockResponse = mock(HttpResponse.class);
                 when(mockResponse.statusCode()).thenReturn(200);
                 when(mockResponse.body()).thenReturn("{\"object\":{\"object\":[]}}");
                 when(mock.searchUsers(anyString())).thenReturn(mockResponse);
             })) {

            mockedConfig.when(ConfigManager::loadConfig).thenReturn(testConfig);

            int exitCode = cmd.execute("-q", "nonexistent");

            assertEquals(0, exitCode);
        }
    }

    @Test
    void testSearchUsersUnauthorized() throws Exception {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class);
             MockedConstruction<UserService> mockedService = mockConstruction(UserService.class, (mock, context) -> {
                 HttpResponse<String> mockResponse = mock(HttpResponse.class);
                 when(mockResponse.statusCode()).thenReturn(401);
                 when(mock.searchUsers(anyString())).thenReturn(mockResponse);
             })) {

            mockedConfig.when(ConfigManager::loadConfig).thenReturn(testConfig);

            int exitCode = cmd.execute("-q", "admin");

            assertEquals(1, exitCode);
        }
    }
}
