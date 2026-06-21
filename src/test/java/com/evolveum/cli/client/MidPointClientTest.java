package com.evolveum.cli.client;

import com.evolveum.cli.exception.MidPointCommunicationException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MidPointClientTest {

    private WireMockServer wireMockServer;
    private MidPointClient client;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0); // Use random available port
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        
        String authHeader = MidPointClient.createBasicAuthHeader("admin", "secret");
        client = new MidPointClient("http://localhost:" + wireMockServer.port() + "/midpoint", authHeader);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testClientInitialization() {
        assertNotNull(client);
    }

    @Test
    void testGetRequest() throws Exception {
        stubFor(get(urlEqualTo("/midpoint/ws/rest/users/123"))
                .withHeader("Authorization", containing("Basic "))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"test-user\"}")));

        HttpResponse<String> response = client.get("/ws/rest/users/123");
        
        assertEquals(200, response.statusCode());
        assertEquals("{\"name\":\"test-user\"}", response.body());
    }

    @Test
    void testGetRequest_Unauthorized() throws Exception {
        stubFor(get(urlEqualTo("/midpoint/ws/rest/users/123"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("Unauthorized access")));

        HttpResponse<String> response = client.get("/ws/rest/users/123");
        
        assertEquals(401, response.statusCode());
        assertEquals("Unauthorized access", response.body());
    }

    @Test
    void testGetRequest_NotFound() throws Exception {
        stubFor(get(urlEqualTo("/midpoint/ws/rest/users/999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("User not found")));

        HttpResponse<String> response = client.get("/ws/rest/users/999");
        
        assertEquals(404, response.statusCode());
        assertEquals("User not found", response.body());
    }

    @Test
    void testSearchRequest() throws Exception {
        stubFor(post(urlEqualTo("/midpoint/ws/rest/users/search"))
                .withHeader("Authorization", containing("Basic "))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson("{\"query\":\"test\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"result\":\"ok\"}")));

        HttpResponse<String> response = client.search("/ws/rest/users/search", "{\"query\":\"test\"}");
        
        assertEquals(200, response.statusCode());
        assertEquals("{\"result\":\"ok\"}", response.body());
    }

    @Test
    void testSearchRequest_ServerError() throws Exception {
        stubFor(post(urlEqualTo("/midpoint/ws/rest/users/search"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        HttpResponse<String> response = client.search("/ws/rest/users/search", "{\"query\":\"test\"}");
        
        assertEquals(500, response.statusCode());
    }

    @Test
    void testPatchRequest() throws Exception {
        stubFor(patch(urlEqualTo("/midpoint/ws/rest/users/123"))
                .withHeader("Authorization", containing("Basic "))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson("{\"modification\":\"test\"}"))
                .willReturn(aResponse()
                        .withStatus(204)));

        HttpResponse<String> response = client.patch("/ws/rest/users/123", "{\"modification\":\"test\"}");
        
        assertEquals(204, response.statusCode());
    }

    @Test
    void testCommunicationException() {
        MidPointClient badClient = new MidPointClient("http://localhost:1/midpoint", "Basic dummy");
        
        assertThrows(MidPointCommunicationException.class, () -> {
            badClient.get("/ws/rest/users/123");
        });
    }
}
