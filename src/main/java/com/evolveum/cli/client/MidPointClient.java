package com.evolveum.cli.client;

import com.evolveum.cli.exception.MidPointCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class MidPointClient implements IMidPointClient {
    private static final Logger logger = LoggerFactory.getLogger(MidPointClient.class);
    private final String baseUrl;
    private final String authHeader;
    private final HttpClient httpClient;

    public MidPointClient(String baseUrl, String authHeader) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.authHeader = authHeader;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static String createBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public HttpResponse<String> get(String path) throws MidPointCommunicationException {
        try {
            logger.debug("GET request to: {}{}", baseUrl, path);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Accept", "application/json")
                    .header("Authorization", authHeader)
                    .GET()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new MidPointCommunicationException("Failed to execute GET request: " + e.getMessage(), e);
        }
    }

    @Override
    public HttpResponse<String> search(String path, String payload) throws MidPointCommunicationException {
        try {
            logger.debug("POST request to: {}{}", baseUrl, path);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", authHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new MidPointCommunicationException("Failed to execute SEARCH request: " + e.getMessage(), e);
        }
    }

    @Override
    public HttpResponse<String> patch(String path, String payload) throws MidPointCommunicationException {
        try {
            logger.debug("PATCH request to: {}{}", baseUrl, path);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authHeader)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new MidPointCommunicationException("Failed to execute PATCH request: " + e.getMessage(), e);
        }
    }
}
