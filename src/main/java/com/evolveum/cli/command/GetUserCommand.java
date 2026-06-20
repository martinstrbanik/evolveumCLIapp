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

@Command(name = "get-user", description = "Get a user object by OID in JSON format")
public class GetUserCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(GetUserCommand.class);

    @Option(names = {"-o", "--oid"}, required = true, description = "The OID of the user to fetch")
    private String oid;

    @Override
    public Integer call() {
        // Validate OID format (typically UUID in midPoint)
        if (!oid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            logger.error("Error: Provided OID is invalid. It should be a UUID format.");
            return 1;
        }

        Properties config;
        try {
            config = ConfigManager.loadConfig();
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
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
            MidPointClient client = new MidPointClient(url, login, password);
            UserService userService = new UserService(client);

            HttpResponse<String> response = userService.getUser(oid);
            logger.info("Received response from midPoint: HTTP {}", response.statusCode());

            // Handle API responses based on HTTP status codes
            switch (response.statusCode()) {
                case 200:
                    System.out.println(response.body());
                    break;
                case 401:
                    logger.error("Error 401: Unauthorized. Please check your login credentials and if REST API is enabled.");
                    break;
                case 403:
                    logger.error("Error 403: Forbidden. Your user does not have required permissions.");
                    break;
                case 404:
                    logger.error("Error 404: Source not found. No user exists with OID: {}", oid);
                    break;
                default:
                    logger.error("Error: Unexpected HTTP code {}", response.statusCode());
                    if (response.body() != null && !response.body().isEmpty()) {
                        logger.error("Message: {}", response.body());
                    }
                    break;
            }

            return (response.statusCode() == 200) ? 0 : 1;

        } catch (Exception e) {
            logger.error("Request failed: {}", e.getMessage());
            return 1;
        }
    }
}

