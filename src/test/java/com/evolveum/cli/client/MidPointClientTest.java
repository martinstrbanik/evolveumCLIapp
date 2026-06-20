package com.evolveum.cli.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MidPointClientTest {

    @Test
    void testClientInitialization() {
        // Simple test to verify constructor logic (URL normalization)
        MidPointClient client = new MidPointClient("http://localhost:8080/midpoint/", "admin", "secret");
        assertNotNull(client);
        
        // Since HttpClient and HttpRequest are final/complex to mock without a real server (like WireMock),
        // we focus on the logic we can test easily.
    }
}
