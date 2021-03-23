package com.company.handlers;

import com.company.QueryNotFoundException;
import com.company.StatusIdResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;

public abstract class AbstractHandler implements HttpHandler {
    protected Connection connection;
    protected static ObjectMapper objectMapper = new ObjectMapper();
    protected boolean isSuccessful = true;
    protected long id = -1;

    public AbstractHandler(Connection connection) {
        this.connection = connection;
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
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

    protected void sendStatus(HttpExchange exchange, boolean isSuccessful, long id) throws IOException {
        StatusIdResponse status = new StatusIdResponse(isSuccessful ? "success" : "fail", id);
        this.isSuccessful = true;
        this.id = -1;
        byte[] response = objectMapper.writeValueAsBytes(status);
        OutputStream outputStream = exchange.getResponseBody();
        exchange.sendResponseHeaders(200, response.length);
        outputStream.write(response);
        outputStream.close();
    }

    public abstract void processGetRequest(HttpExchange exchange) throws IOException;

    public abstract void processPostRequest(HttpExchange exchange) throws IOException;
}
