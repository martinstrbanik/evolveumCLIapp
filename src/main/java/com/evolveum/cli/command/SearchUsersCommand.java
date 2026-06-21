package com.evolveum.cli.command;

import com.evolveum.cli.client.MidPointClient;
import com.evolveum.cli.config.ConfigManager;
import com.evolveum.cli.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.concurrent.Callable;

@Command(name = "search-users", description = "Search users by name/login and display name and OID")
public class SearchUsersCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(SearchUsersCommand.class);

    @Option(names = {"-c", "--config"}, description = "Path to config file (default: ~/.evcliapp.properties)")
    private String configPath;

    @Option(names = {"-q", "--query"}, required = true, description = "The search query (e.g., part of the username)")
    private String query;

    @Override
    public Integer call() {
        Properties config;
        try {
            String resolvedPath = ConfigManager.resolveConfigPath(configPath);
            config = ConfigManager.loadConfig(resolvedPath);
        } catch (com.evolveum.cli.exception.ConfigurationNotFoundException e) {
            System.err.println(e.getMessage());
            logger.error("Configuration error: {}", e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Failed to load configuration. Check logs/app.log for details.");
            logger.error("Error loading config: {}", e.getMessage(), e);
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
            String authHeader = MidPointClient.createBasicAuthHeader(login, password);
            MidPointClient client = new MidPointClient(url, authHeader);
            UserService userService = new UserService(client);

            logger.info("Searching for users...");
            HttpResponse<String> response = userService.searchUsers(query);
            logger.info("Received response from midPoint: HTTP {}", response.statusCode());

            // Handle API responses
            switch (response.statusCode()) {
                case 200:
                    parseAndDisplayResults(response.body());
                    break;
                case 401:
                    logger.error("Error 401: Unauthorized. Please check your login credentials.");
                    System.err.println("Error 401: Unauthorized. Check logs/app.log for details.");
                    break;
                case 403:
                    logger.error("Error 403: Forbidden. Your user does not have required permissions.");
                    System.err.println("Error 403: Forbidden. Check logs/app.log for details.");
                    break;
                default:
                    logger.error("Error: Unexpected HTTP code {}", response.statusCode());
                    if (response.body() != null && !response.body().isEmpty()) {
                        logger.error("Details: {}", response.body());
                    }
                    System.err.println("Error: Unexpected HTTP code " + response.statusCode() + ". Please check the log file (logs/app.log) for more details.");
                    break;
            }

            return (response.statusCode() == 200) ? 0 : 1;

        } catch (com.evolveum.cli.exception.MidPointCommunicationException e) {
            logger.error("MidPoint communication failed: {}", e.getMessage(), e);
            System.err.println("Communication error: " + e.getMessage() + ". Please check the log file (logs/app.log) for more details.");
            return 1;
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            System.err.println("Unexpected error occurred: " + e.getMessage() + ". Please check the log file (logs/app.log) for more details.");
            return 1;
        }
    }

    private void parseAndDisplayResults(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            // midPoint search returns: { "object": { "object": [ ... ] } }
            JsonNode objectsArray = root.path("object").path("object");

            if (objectsArray.isMissingNode() || !objectsArray.isArray() || objectsArray.isEmpty()) {
                System.out.println("No users found matching the query.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\nMatching users found:\n");
            sb.append("----------------------------------------------------------------------\n");
            sb.append(String.format("%-25s | %s\n", "Username / Login", "OID"));
            sb.append("----------------------------------------------------------------------\n");

            int count = 0;
            for (JsonNode userNode : objectsArray) {
                String oid = userNode.path("oid").asText("N/A");
                
                // Name can be a String or a PolyString object {"orig": "..."}
                JsonNode nameNode = userNode.path("name");
                String name = "N/A";
                if (nameNode.isObject()) {
                    name = nameNode.path("orig").asText("N/A");
                } else if (nameNode.isTextual()) {
                    name = nameNode.asText();
                }

                sb.append(String.format("%-25s | %s\n", name, oid));
                count++;
            }

            sb.append("----------------------------------------------------------------------\n");
            sb.append("Total: ").append(count).append(" user(s) found.");
            
            System.out.println(sb.toString());

        } catch (Exception e) {
            logger.error("Failed to parse search results: {}", e.getMessage());
        }
    }
}

