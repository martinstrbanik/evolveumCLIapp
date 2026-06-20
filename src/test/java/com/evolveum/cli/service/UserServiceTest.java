package com.evolveum.cli.service;

import com.evolveum.cli.client.MidPointClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock
    private MidPointClient mockClient;

    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(mockClient);
    }

    @Test
    void testGetUser() throws Exception {
        // Given
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"name\":\"test\"}");
        when(mockClient.get(contains("/ws/rest/users/123"))).thenReturn(mockResponse);

        // When
        HttpResponse<String> response = userService.getUser("123");

        // Then
        assertEquals(200, response.statusCode());
        assertEquals("{\"name\":\"test\"}", response.body());
    }

    @Test
    void testSearchUsers() throws Exception {
        // Given
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockClient.search(anyString(), anyString())).thenReturn(mockResponse);

        // When
        HttpResponse<String> response = userService.searchUsers("peter");

        // Then
        assertEquals(200, response.statusCode());
    }

    @Test
    void testModifyUser() throws Exception {
        // Given
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(204);
        when(mockClient.patch(anyString(), anyString())).thenReturn(mockResponse);

        // When
        HttpResponse<String> response = userService.modifyUser("123", "description", "new", "replace");

        // Then
        assertEquals(204, response.statusCode());
    }
}
