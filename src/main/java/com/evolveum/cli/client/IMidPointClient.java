package com.evolveum.cli.client;

import java.net.http.HttpResponse;
import com.evolveum.cli.exception.MidPointCommunicationException;

public interface IMidPointClient {
    HttpResponse<String> get(String path) throws MidPointCommunicationException;
    HttpResponse<String> search(String path, String payload) throws MidPointCommunicationException;
    HttpResponse<String> patch(String path, String payload) throws MidPointCommunicationException;
}
