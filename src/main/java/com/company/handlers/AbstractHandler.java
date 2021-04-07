package com.company.handlers;

import com.company.QueryNotFoundException;
import com.company.StatusResponse;
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

    protected void sendStatus(HttpExchange exchange, boolean isSuccessful, long... idAndOptionalTime) throws IOException {
        StatusResponse status;
        if (idAndOptionalTime.length == 1)
             status = new StatusResponse(isSuccessful ? "success" : "fail", idAndOptionalTime[0]);
        else
            status = new StatusResponse(isSuccessful ? "success" : "fail", idAndOptionalTime[0], idAndOptionalTime[1]);
        this.isSuccessful = true;
        byte[] response = objectMapper.writeValueAsBytes(status);
        OutputStream outputStream = exchange.getResponseBody();
        exchange.sendResponseHeaders(200, response.length);
        outputStream.write(response);

        outputStream.close();
    }

    public abstract void processGetRequest(HttpExchange exchange) throws IOException;

    public abstract void processPostRequest(HttpExchange exchange) throws IOException;
}
