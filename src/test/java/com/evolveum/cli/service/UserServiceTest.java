package com.evolveum.cli.service;

import com.evolveum.cli.client.IMidPointClient;
import com.evolveum.cli.exception.MidPointCommunicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock
    private IMidPointClient mockClient;

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

    @Test
    void testSearchUsers_PayloadContainsEscapedQuotes() throws Exception {
        // Given
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        when(mockClient.search(eq("/ws/rest/users/search"), payloadCaptor.capture())).thenReturn(mockResponse);

        // When
        HttpResponse<String> response = userService.searchUsers("pe\"ter");

        // Then
        assertEquals(200, response.statusCode());
        String capturedPayload = payloadCaptor.getValue();
        assertTrue(capturedPayload.contains("pe\\\\\\\"ter"), "Payload should correctly escape quotes.");
    }

    @Test
    void testModifyUser_PayloadGeneration() throws Exception {
        // Given
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(204);
        
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        when(mockClient.patch(contains("123"), payloadCaptor.capture())).thenReturn(mockResponse);

        // When
        userService.modifyUser("123", "extension/costCenter", "1000", "add");

        // Then
        String payload = payloadCaptor.getValue();
        assertTrue(payload.contains("\"modificationType\":\"add\""));
        assertTrue(payload.contains("\"path\":\"extension/costCenter\""));
        assertTrue(payload.contains("\"value\":\"1000\""));
    }

    @Test
    void testClientThrowsException_WrappedInService() throws Exception {
        // Given:
        when(mockClient.get(anyString())).thenThrow(new MidPointCommunicationException("Network timeout"));

        // When / Then:
        assertThrows(MidPointCommunicationException.class, () -> {
            userService.getUser("123");
        });
    }

    @Test
    void testSearchUsers_NullQuery() throws Exception {
        // Given
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        when(mockClient.search(anyString(), payloadCaptor.capture())).thenReturn(mockResponse);

        // When
        userService.searchUsers(null);

        // Then
        String payload = payloadCaptor.getValue();
        // null input
        assertTrue(payload.contains("name contains[origIgnoreCase] \\\"\\\""));
    }
}
