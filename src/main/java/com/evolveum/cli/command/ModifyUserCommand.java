package com.evolveum.cli.command;

import com.evolveum.cli.client.MidPointClient;
import com.evolveum.cli.config.ConfigManager;
import com.evolveum.cli.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.concurrent.Callable;

@Command(name = "modify-user", description = "Modify a user attribute by OID")
public class ModifyUserCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ModifyUserCommand.class);

    @Option(names = {"-c", "--config"}, description = "Path to config file (default: ~/.evcliapp.properties)")
    private String configPath;

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
            System.err.println("Error: Provided OID is invalid. It should be a UUID format.");
            return 1;
        }

        // Validate modification type
        if (!type.equalsIgnoreCase("add") && !type.equalsIgnoreCase("replace") && !type.equalsIgnoreCase("delete")) {
            logger.error("Error: Invalid modification type. Allowed values are: add, replace, delete.");
            System.err.println("Error: Invalid modification type. Allowed values are: add, replace, delete.");
            return 1;
        }

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

            System.out.println("Sending modification request...");
            HttpResponse<String> response = userService.modifyUser(oid, path, value, type);
            logger.info("Received response from midPoint: HTTP {}", response.statusCode());

            // Handle API responses
            switch (response.statusCode()) {
                case 200:
                case 204:
                    System.out.println("Success: User modified successfully (HTTP " + response.statusCode() + ").");
                    break;
                case 400:
                    logger.error("Error 400: Bad Request. Please check if the path '{}' is valid and the value is correct.", path);
                    System.err.println("Error 400: Bad Request. Check logs/app.log for details.");
                    if (response.body() != null && !response.body().isEmpty()) {
                        logger.error("Details: {}", response.body());
                    }
                    break;
                case 401:
                    logger.error("Error 401: Unauthorized. Please check your login credentials.");
                    System.err.println("Error 401: Unauthorized. Check logs/app.log for details.");
                    break;
                case 403:
                    logger.error("Error 403: Forbidden. Your user does not have required permissions.");
                    System.err.println("Error 403: Forbidden. Check logs/app.log for details.");
                    break;
                case 404:
                    logger.error("Error 404: User not found with OID: {}", oid);
                    System.err.println("Error 404: User not found. Check logs/app.log for details.");
                    break;
                case 409:
                    logger.error("Error 409: Conflict. The modification violates midPoint constraints.");
                    System.err.println("Error 409: Conflict. Check logs/app.log for details.");
                    if (response.body() != null && !response.body().isEmpty()) {
                        logger.error("Details: {}", response.body());
                    }
                    break;
                default:
                    logger.error("Error: Unexpected HTTP code {}", response.statusCode());
                    System.err.println("Error: Unexpected HTTP code " + response.statusCode() + ". Please check the log file (logs/app.log) for more details.");
                    if (response.body() != null && !response.body().isEmpty()) {
                        logger.error("Details: {}", response.body());
                    }
                    break;
            }

            return (response.statusCode() == 200 || response.statusCode() == 204) ? 0 : 1;

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
}

