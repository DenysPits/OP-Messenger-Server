package com.company.handlers;

import com.company.QueryNotFoundException;
import com.company.status.Status;
import com.company.status.StatusResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractHandler implements HttpHandler {
    protected static ObjectMapper objectMapper = new ObjectMapper();
    protected Status status = Status.SUCCESS;

    public AbstractHandler() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
    }

    @Override
    public synchronized void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase("post"))
            processPostRequest(exchange);
        else if (exchange.getRequestMethod().equalsIgnoreCase("get"))
            processGetRequest(exchange);
    }

    protected String getQueryValue(String query, String key) throws QueryNotFoundException {
        if (query == null || key == null)
            throw new QueryNotFoundException();
        String[] queryElements = query.split("&");
        for (String queryElement : queryElements) {
            String[] keyAndValue = queryElement.split("=");
            if (keyAndValue[0].equalsIgnoreCase(key))
                return keyAndValue[1];
        }
        throw new QueryNotFoundException();
    }

    protected StatusResponse getStatusResponse(long id) {
        return new StatusResponse(status, id);
    }

    protected StatusResponse getStatusResponse(long id, long time) {
        return new StatusResponse(status, id, time);
    }

    protected void sendStatus(HttpExchange exchange, StatusResponse statusResponse) throws IOException {
        this.status = Status.SUCCESS; //don't delete, it is useful
        byte[] response = objectMapper.writeValueAsBytes(statusResponse);
        OutputStream outputStream = exchange.getResponseBody();
        exchange.sendResponseHeaders(200, response.length);
        outputStream.write(response);
        outputStream.close();
    }

    protected <T> void sendEntitiesInResponse(HttpExchange exchange, T entity) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(entity);
        exchange.sendResponseHeaders(200, response.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response);
        outputStream.close();
    }

    public abstract void processGetRequest(HttpExchange exchange) throws IOException;

    public abstract void processPostRequest(HttpExchange exchange) throws IOException;
}
