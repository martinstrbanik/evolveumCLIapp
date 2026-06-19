package com.evolveum.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.Callable;

@Command(name = "modify-user", description = "Modify a user attribute by OID")
public class ModifyUserCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ModifyUserCommand.class);

    @Option(names = {"-o", "--oid"}, required = true, description = "The OID of the user to modify")
    private String oid;

    @Option(names = {"-p", "--path"}, required = true, description = "The attribute path to modify (e.g., description, givenName)")
    private String path;

    @Option(names = {"-v", "--value"}, required = true, description = "The new value for the attribute")
    private String value;

    @Option(names = {"-t", "--type"}, defaultValue = "replace", description = "Modification type: add, replace, delete (default: replace)")
    private String type;

    @Override
    public Integer call() {
        // Validate OID format
        if (!oid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            logger.error("Error: Provided OID is invalid. It should be a UUID format.");
            return 1;
        }

        // Validate modification type
        if (!type.equalsIgnoreCase("add") && !type.equalsIgnoreCase("replace") && !type.equalsIgnoreCase("delete")) {
            logger.error("Error: Invalid modification type. Allowed values are: add, replace, delete.");
            return 1;
        }

        Properties config;
        try {
            config = ConfigManager.loadConfig();
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage());
            return 1;
        }

        String url = config.getProperty("url");
        String login = config.getProperty("login");
        String password = config.getProperty("password");

        if (url == null || login == null || password == null) {
            logger.error("Configuration is incomplete. Please run 'config-init' again.");
            return 1;
        }

        try {
            // Build Base64 Auth header
            String authData = login + ":" + password;
            String base64Auth = Base64.getEncoder().encodeToString(authData.getBytes(StandardCharsets.UTF_8));

            String targetEndpoint = url + "/ws/rest/users/" + oid;

            // Prepare JSON payload using template
            String jsonTemplate = """
            {
              "objectModification": {
                "itemDelta": {
                  "modificationType": "%s",
                  "path": "%s",
                  "value": "%s"
                }
              }
            }
            """;

            String payload = String.format(jsonTemplate, type.toLowerCase(), path, escapeJson(value));

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetEndpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + base64Auth)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            System.out.println("Sending modification request...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Handle API responses
            switch (response.statusCode()) {
                case 200:
                case 204:
                    System.out.println("Success: User modified successfully (HTTP " + response.statusCode() + ").");
                    break;
                case 400:
                    logger.error("Error 400: Bad Request. Please check if the path '" + path + "' is valid and the value is correct.");
                    if (response.body() != null && !response.body().isEmpty()) {
                        logger.error("Details: " + response.body());
                    }
                    break;
                case 401:
                    logger.error("Error 401: Unauthorized. Please check your login credentials.");
                    break;
                case 403:
                    logger.error("Error 403: Forbidden. Your user does not have required permissions.");
                    break;
                case 404:
                    logger.error("Error 404: User not found with OID: " + oid);
                    break;
                case 409:
                    logger.error("Error 409: Conflict. The modification violates midPoint constraints.");
                    if (response.body() != null && !response.body().isEmpty()) {
                        logger.error("Details: " + response.body());
                    }
                    break;
                default:
                    logger.error("Error: Unexpected HTTP code " + response.statusCode());
                    if (response.body() != null && !response.body().isEmpty()) {
                        logger.error("Details: " + response.body());
                    }
                    break;
            }

            return (response.statusCode() == 200 || response.statusCode() == 204) ? 0 : 1;

        } catch (Exception e) {
            logger.error("Request failed: " + e.getMessage());
            return 1;
        }
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    // Reference: JSON allows any other unicode character directly
                    if (ch < ' ') {
                        String t = "000" + Integer.toHexString(ch);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
