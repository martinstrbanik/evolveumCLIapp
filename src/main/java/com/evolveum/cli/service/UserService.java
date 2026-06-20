package com.evolveum.cli.service;

import com.evolveum.cli.client.IMidPointClient;
import com.evolveum.cli.exception.MidPointCommunicationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;

public class UserService implements IUserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final IMidPointClient client;
    private final ObjectMapper objectMapper;

    public UserService(IMidPointClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public HttpResponse<String> getUser(String oid) throws MidPointCommunicationException {
        logger.info("Fetching user with OID: {}", oid);
        return client.get("/ws/rest/users/" + oid + "?exclude=@metadata");
    }

    @Override
    public HttpResponse<String> searchUsers(String query) throws MidPointCommunicationException {
        logger.info("Searching users with query: {}", query);
        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            ObjectNode queryNode = rootNode.putObject("query");
            ObjectNode filterNode = queryNode.putObject("filter");
            filterNode.put("text", "name contains[origIgnoreCase] \"" + escapeQuotes(query) + "\"");

            String payload = objectMapper.writeValueAsString(rootNode);
            return client.search("/ws/rest/users/search", payload);
        } catch (Exception e) {
            throw new MidPointCommunicationException("Error creating search payload", e);
        }
    }

    @Override
    public HttpResponse<String> modifyUser(String oid, String path, String value, String type) throws MidPointCommunicationException {
        logger.info("Modifying user OID: {}, path: {}, type: {}", oid, path, type);
        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            ObjectNode objectModNode = rootNode.putObject("objectModification");
            ObjectNode itemDeltaNode = objectModNode.putObject("itemDelta");

            itemDeltaNode.put("modificationType", type.toLowerCase());
            itemDeltaNode.put("path", path);
            itemDeltaNode.put("value", value);

            String payload = objectMapper.writeValueAsString(rootNode);
            return client.patch("/ws/rest/users/" + oid, payload);
        } catch (Exception e) {
            throw new MidPointCommunicationException("Error creating modification payload", e);
        }
    }

    private String escapeQuotes(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"");
    }
}
