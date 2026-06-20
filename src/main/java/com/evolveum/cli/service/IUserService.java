package com.evolveum.cli.service;

import java.net.http.HttpResponse;
import com.evolveum.cli.exception.MidPointCommunicationException;

public interface IUserService {
    HttpResponse<String> getUser(String oid) throws MidPointCommunicationException;
    HttpResponse<String> searchUsers(String query) throws MidPointCommunicationException;
    HttpResponse<String> modifyUser(String oid, String path, String value, String type) throws MidPointCommunicationException;
}
