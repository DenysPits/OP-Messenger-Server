package com.company.handlers;

import com.company.Message;
import com.company.QueryNotFoundException;
import com.company.Status;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    protected static Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    public MessageHandler(Connection connection) {
        super(connection);
    }

    @Override
    public void processGetRequest(HttpExchange exchange) throws IOException {
        ArrayList<Message> messages = new ArrayList<>();
        try (InputStream inputStream = exchange.getRequestBody()) {
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
        Message message = null;
        try (InputStream inputStream = exchange.getRequestBody()) {
            logger.trace("Got request body");
            message = objectMapper.readValue(inputStream, Message.class);
            logger.trace("Deserialized json");
            processAddStatement(message);
            logger.trace("Processed add statement");
        } catch (Exception e) {
            status = Status.FAIL;
            logger.warn("Exception was caught and status fail was assigned");
            if (message != null) {
                message.setTime(0);
                message.setId(-1);
            }
            logger.trace("Time was set to 0, the id was set to -1");
            e.printStackTrace();
        } finally {
            if (message == null)
                sendStatus(exchange, getStatusResponse(-1, -1));
            else
                sendStatus(exchange, getStatusResponse(message.getId(), message.getTime()));
        }
    }

    private void processAddStatement(Message message) throws Exception {
        try (PreparedStatement addStatement =
                     connection.prepareStatement(ADD, Statement.RETURN_GENERATED_KEYS)) {
            logger.trace("Statement prepared");
            addStatement.setLong(1, message.getFromId());
            addStatement.setLong(2, message.getToId());
            addStatement.setString(3, message.getBody());
            addStatement.setLong(4, message.getTime());
            addStatement.setString(5, message.getAction());
            logger.trace("Properties were set");
            addStatement.executeUpdate();
            logger.trace("Add statement was executed");
            addStatement.getGeneratedKeys();
            ResultSet resultSet = addStatement.getGeneratedKeys();
            if (resultSet.next()) {
                message.setId(resultSet.getLong(1));
            }
            logger.trace("Returned id was set");
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