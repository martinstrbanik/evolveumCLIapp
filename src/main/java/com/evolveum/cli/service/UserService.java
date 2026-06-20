package com.evolveum.cli.service;

import com.evolveum.cli.client.MidPointClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final MidPointClient client;

    public UserService(MidPointClient client) {
        this.client = client;
    }

    public HttpResponse<String> getUser(String oid) throws Exception {
        logger.info("Fetching user with OID: {}", oid);
        return client.get("/ws/rest/users/" + oid + "?exclude=@metadata");
    }

    public HttpResponse<String> searchUsers(String query) throws Exception {
        logger.info("Searching users with query: {}", query);
        String jsonTemplate = """
        {
          "query": {
            "filter": {
              "text": "name contains[origIgnoreCase] \\"%s\\""
            }
          }
        }
        """;
        String payload = String.format(jsonTemplate, escapeJson(query));
        return client.search("/ws/rest/users/search", payload);
    }

    public HttpResponse<String> modifyUser(String oid, String path, String value, String type) throws Exception {
        logger.info("Modifying user OID: {}, path: {}, type: {}", oid, path, type);
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
        return client.patch("/ws/rest/users/" + oid, payload);
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < ' ') {
                        String t = "000" + Integer.toHexString(ch);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }
}
