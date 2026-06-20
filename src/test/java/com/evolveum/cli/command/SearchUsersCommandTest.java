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

class SearchUsersCommandTest {

    private SearchUsersCommand searchUsersCommand;
    private CommandLine cmd;
    private Properties testConfig;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        searchUsersCommand = new SearchUsersCommand();
        cmd = new CommandLine(searchUsersCommand);

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
    void testSearchUsersSuccess() throws Exception {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class);
             MockedConstruction<UserService> mockedService = mockConstruction(UserService.class, (mock, context) -> {
                 HttpResponse<String> mockResponse = mock(HttpResponse.class);
                 when(mockResponse.statusCode()).thenReturn(200);
                 when(mockResponse.body()).thenReturn("{\"object\":{\"object\":[{\"oid\":\"123\",\"name\":\"peter\"}]}}");
                 when(mock.searchUsers(anyString())).thenReturn(mockResponse);
             })) {

            mockedConfig.when(ConfigManager::loadConfig).thenReturn(testConfig);

            int exitCode = cmd.execute("-q", "peter");

            assertEquals(0, exitCode);
            verify(mockedService.constructed().get(0)).searchUsers("peter");
            
            String output = outContent.toString();
            assertTrue(output.contains("peter"));
            assertTrue(output.contains("123"));
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
            assertTrue(outContent.toString().contains("No users found matching the query."));
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

    @Test
    void testSearchUsersBadJsonFormat() throws Exception {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class);
             MockedConstruction<UserService> mockedService = mockConstruction(UserService.class, (mock, context) -> {
                 HttpResponse<String> mockResponse = mock(HttpResponse.class);
                 when(mockResponse.statusCode()).thenReturn(200);
                 when(mockResponse.body()).thenReturn("{ invalid json ");
                 when(mock.searchUsers(anyString())).thenReturn(mockResponse);
             })) {

            mockedConfig.when(ConfigManager::loadConfig).thenReturn(testConfig);

            int exitCode = cmd.execute("-q", "badformat");

            assertEquals(0, exitCode);
        }
    }
}