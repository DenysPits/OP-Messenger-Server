package com.company.handlers;

import com.company.Message;
import com.company.QueryNotFoundException;
import com.company.StatusIdResponse;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageHandler extends AbstractHandler {
    public static final String ADD = "INSERT INTO messages (from_id, to_id, body, time, action) VALUES (?, ?, ?, ?, ?);";
    public static final String GET = "SELECT * FROM messages WHERE to_id = ?;";
    public static final String DELETE = "DELETE FROM messages WHERE to_id = ?";

    public MessageHandler(Connection connection) {
        super(connection);
    }

    @Override
    public void processGetRequest(HttpExchange exchange) throws IOException {
        ArrayList<Message> messages = new ArrayList<>();
        try (InputStream inputStream = exchange.getRequestBody();) {
            String requestQuery = exchange.getRequestURI().getQuery();
            long toId = Long.parseLong(getQueryValue(requestQuery, "id"));
            boolean deleteMessages;
            try {
                deleteMessages = Boolean.parseBoolean(getQueryValue(requestQuery, "del"));
            } catch (QueryNotFoundException e) {
                deleteMessages = false;
            }
            processGetStatement(toId, messages);
            if (deleteMessages)
                processDeleteStatement(toId);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sendMessagesInResponse(exchange, messages);
        }
    }

    @Override
    public void processPostRequest(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            Message message = objectMapper.readValue(inputStream, Message.class);
            processAddStatement(message);
        } catch (Exception e) {
            isSuccessful = false;
            e.printStackTrace();
        } finally {
            sendStatus(exchange, isSuccessful, id);
        }
    }

    private void processAddStatement(Message message) {
        try (PreparedStatement addStatement =
                     connection.prepareStatement(ADD, Statement.RETURN_GENERATED_KEYS)) {
            addStatement.setLong(1, message.getFromId());
            addStatement.setLong(2, message.getToId());
            addStatement.setString(3, message.getBody());
            addStatement.setLong(4, message.getTime());
            addStatement.setString(5, message.getAction());
            addStatement.executeUpdate();
            addStatement.getGeneratedKeys();
            ResultSet resultSet = addStatement.getGeneratedKeys();
            if (resultSet.next())
                id = resultSet.getLong(1);
        } catch (Exception e) {
            isSuccessful = false;
            e.printStackTrace();
        }
    }

    private void processDeleteStatement(long toId) {
        try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE)) {
            deleteStatement.setLong(1, toId);
            deleteStatement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void processGetStatement(long toId, List<Message> messages) {
        try (PreparedStatement getStatement = connection.prepareStatement(GET)) {
            getStatement.setLong(1, toId);
            ResultSet resultSet = getStatement.executeQuery();
            while (resultSet.next()) {
                Message message = new Message();
                message.setId(resultSet.getLong("id"));
                message.setFromId(resultSet.getLong("from_id"));
                message.setToId(resultSet.getLong("to_id"));
                message.setBody(resultSet.getString("body"));
                message.setTime(resultSet.getLong("time"));
                message.setAction(resultSet.getString("action"));
                messages.add(message);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void sendMessagesInResponse(HttpExchange exchange, List<Message> messages) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(messages);
        exchange.sendResponseHeaders(200, response.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response);
        outputStream.close();
    }
}