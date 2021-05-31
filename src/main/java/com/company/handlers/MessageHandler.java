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
import java.util.BitSet;
import java.util.List;

public class MessageHandler extends AbstractHandler {
    public static final String ADD = "INSERT INTO messages (from_id, to_id, body, time, action) VALUES (?, ?, ?, ?, ?);";
    public static final String GET_MESSAGES_TO_ID = "SELECT * FROM messages WHERE to_id = ?;";
    public static final String GET_ALL = "SELECT * FROM messages;";
    public static final String DELETE = "DELETE FROM messages WHERE to_id = ?";
    protected static Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private static MessageHandler instance;
    private BitSet messagesInDatabase = new BitSet();

    private MessageHandler(Connection connection) {
        super(connection);
        populateMessagesBitSet();
    }

    public static MessageHandler getInstance(Connection connection) {
        if (instance == null) {
            synchronized (MessageHandler.class) {
                if (instance == null) {
                    instance = new MessageHandler(connection);
                }
            }
        }
        return instance;
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
            if (isMessagesInBitset(toId)) {
                doGetToIdStatement(toId, messages);
                if (deleteMessages)
                    processDeleteStatement(toId);
            }
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
            message = objectMapper.readValue(inputStream, Message.class);
            processAddStatement(message);
            UserHandler.getInstance(connection).addUsersRelationship(message);
        } catch (Exception e) {
            logger.warn("Exception was caught", e);
            status = Status.FAIL;
            if (message != null) {
                message.setTime(0);
                message.setId(-1);
            }
            e.printStackTrace();
        } finally {
            if (message == null) {
                sendStatus(exchange, getStatusResponse(-1, 0));
            }
            else {
                sendStatus(exchange, getStatusResponse(message.getId(), message.getTime()));
            }
        }
    }

    public void processAddStatement(Message message) throws Exception {
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
            if (resultSet.next()) {
                long id = resultSet.getLong(1);
                message.setId(id);
            }
            addMessageToBitset(message.getToId());
        }
    }

    public void doGetToIdStatement(long toId, List<Message> messages) {
        try (PreparedStatement getStatement = connection.prepareStatement(GET_MESSAGES_TO_ID)) {
            getStatement.setLong(1, toId);
            ResultSet resultSet = getStatement.executeQuery();
            addRetrievedMessagesToList(messages, resultSet);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void populateMessagesBitSet() {
        try (PreparedStatement getStatement = connection.prepareStatement(GET_ALL)) {
            ResultSet resultSet = getStatement.executeQuery();
            while (resultSet.next()) {
                messagesInDatabase.set((int) resultSet.getLong("to_id"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void addMessageToBitset(long id) {
        messagesInDatabase.set((int) id);
    }

    private boolean isMessagesInBitset(long id) {
        return messagesInDatabase.get((int) id);
    }

    private void deleteMessageInBitset(long id) {
        messagesInDatabase.clear((int) id);
    }

    private void processDeleteStatement(long toId) {
        try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE)) {
            deleteStatement.setLong(1, toId);
            deleteStatement.executeUpdate();
            deleteMessageInBitset(toId);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void addRetrievedMessagesToList(List<Message> messages, ResultSet resultSet) throws SQLException {
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
    }

    private void sendMessagesInResponse(HttpExchange exchange, List<Message> messages) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(messages);
        exchange.sendResponseHeaders(200, response.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response);
        outputStream.close();
    }
}