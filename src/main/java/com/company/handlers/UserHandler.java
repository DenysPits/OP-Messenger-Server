package com.company.handlers;

import com.company.Message;
import com.company.QueryNotFoundException;
import com.company.Status;
import com.company.User;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;

public class UserHandler extends AbstractHandler {
    private static final String ADD = "INSERT INTO users (name, tag, public_rsa, photo) VALUES (?, ?, ?, ?);";
    private static final String UPDATE = "UPDATE users SET name=?, tag=?, photo=? WHERE id=?";
    private static final String GET_BY_ID = "SELECT * FROM users WHERE id=?";
    private static final String GET_BY_TAG = "SELECT * FROM users WHERE tag=?";
    private static final String SELECT_RELATIONSHIPS = "SELECT relationships FROM users WHERE id=?";
    private static UserHandler instance;

    private UserHandler(Connection connection) {
        super(connection);
    }

    public static UserHandler getInstance(Connection connection) {
        if (instance == null) {
            synchronized (UserHandler.class) {
                if (instance == null) {
                    instance = new UserHandler(connection);
                }
            }
        }
        return instance;
    }

    @Override
    public void processGetRequest(HttpExchange exchange) {
        String requestQuery = exchange.getRequestURI().getQuery();
        String[] queryKeys = getQueryKeys(requestQuery);
        User user = null;
        for (String queryKey : queryKeys) {
            if (queryKey.equals("id")) {
                user = getUserById(exchange, requestQuery);
                break;
            } else if (queryKey.equals("tag")) {
                user = getUserByTag(exchange, requestQuery);
                break;
            }
        }
        sendUserInResponse(exchange, user);
    }

    @Override
    public void processPostRequest(HttpExchange exchange) throws IOException {
        User user = null;
        try (InputStream inputStream = exchange.getRequestBody()) {
            String requestQuery = exchange.getRequestURI().getQuery();
            user = objectMapper.readValue(inputStream, User.class);
            user.setId(Long.parseLong(getQueryValue(requestQuery, "update")));
            processUpdateStatement(user);
        } catch (QueryNotFoundException queryNotFoundException) {
            processAddStatement(user);
        } finally {
            sendStatus(exchange, getStatusResponse(user != null ? user.getId() : -1));
        }
    }

    public void addUsersRelationship(Message message) {
        String addRelationshipForSender =
                "UPDATE users SET relationships=CONCAT(IFNULL(relationships,''),'" + message.getToId() + " ') WHERE " +
                        "id=" + message.getFromId();
        String addRelationshipForReceiver =
                "UPDATE users SET relationships=CONCAT(IFNULL(relationships,''),'" + message.getFromId() + " ') WHERE" +
                        " id=" + message.getToId();
        try (Statement addRelationshipStatement = connection.createStatement()) {
            String userRelationships = getUserRelationships(message.getFromId());
            if (!checkSameRelationships(userRelationships, message.getToId())) {
                addRelationshipStatement.executeUpdate(addRelationshipForSender);
                addRelationshipStatement.executeUpdate(addRelationshipForReceiver);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String getUserRelationships(long id) {
        try (PreparedStatement selectRelationshipsStatement = connection.prepareStatement(SELECT_RELATIONSHIPS)) {
            selectRelationshipsStatement.setLong(1, id);
            ResultSet relationsSet = selectRelationshipsStatement.executeQuery();
            if (relationsSet.next()) {
                return relationsSet.getString(1);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    private boolean checkSameRelationships(String relationshipsText, long toId) throws SQLException {
        if (relationshipsText != null) {
            String[] relationsSplit = relationshipsText.trim().split("\\s+");
            for (String relation : relationsSplit) {
                if (relation.equals(String.valueOf(toId))) {
                    return true;
                }
            }
        }
        return false;
    }

    private User getUserById(HttpExchange exchange, String query) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(GET_BY_ID)) {
            preparedStatement.setString(1, getQueryValue(query, "id"));
            return makeUserFromResultSet(preparedStatement);
        } catch (SQLException | QueryNotFoundException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    private User getUserByTag(HttpExchange exchange, String query) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(GET_BY_TAG)) {
            preparedStatement.setString(1, getQueryValue(query, "tag"));
            return makeUserFromResultSet(preparedStatement);
        } catch (SQLException | QueryNotFoundException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    private void sendUserInResponse(HttpExchange exchange, User user) {
        try {
            byte[] response = objectMapper.writeValueAsBytes(user);
            exchange.sendResponseHeaders(200, response.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private User makeUserFromResultSet(PreparedStatement preparedStatement) {
        try {
            ResultSet userResultSet = preparedStatement.executeQuery();
            User user = null;
            while (userResultSet.next()) {
                user = new User();
                user.setId(userResultSet.getLong("id"));
                user.setName(userResultSet.getString("name"));
                user.setPhoto(userResultSet.getString("photo"));
                user.setPublicRsa(userResultSet.getString("public_rsa"));
                user.setTag(userResultSet.getString("tag"));
            }
            return user;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    private String[] getQueryKeys(String query) {
        String[] queryElements = query.split("&");
        String[] queryKeys = new String[queryElements.length];
        for (int i = 0; i < queryKeys.length; i++) {
            queryKeys[i] = queryElements[i].substring(0, queryElements[i].indexOf("="));
        }
        return queryKeys;
    }

    private void processUpdateStatement(User user) {
        try (PreparedStatement updateStatement = connection.prepareStatement(UPDATE)) {
            updateStatement.setString(1, user.getName());
            updateStatement.setString(2, user.getTag());
            updateStatement.setString(3, user.getPhoto());
            updateStatement.setLong(4, user.getId());
            updateStatement.executeUpdate();
            notifyPeople(user.getId());
        } catch (SQLIntegrityConstraintViolationException e) {
            status = Status.TAG_IS_TAKEN;
            e.printStackTrace();
        } catch (Exception throwables) {
            status = Status.FAIL;
            throwables.printStackTrace();
        }
    }

    private void notifyPeople(long idWhoWasUpdated) throws Exception {
        String userRelationshipsStr = getUserRelationships(idWhoWasUpdated);
        if (userRelationshipsStr == null) {
            return;
        }
        Message updateMessage = new Message();
        updateMessage.setAction("update");
        updateMessage.setFromId(0);
        updateMessage.setBody(String.valueOf(idWhoWasUpdated));
        String[] userRelationships = userRelationshipsStr.trim().split("\\s+");
        MessageHandler messageHandler = MessageHandler.getInstance(connection);
        for (String userRelationship : userRelationships) {
            updateMessage.setToId(Long.parseLong(userRelationship));
            messageHandler.processAddStatement(updateMessage);
        }
    }

    private void processAddStatement(User user) {
        try (PreparedStatement addStatement = connection.prepareStatement(ADD, Statement.RETURN_GENERATED_KEYS)) {
            addStatement.setString(1, user.getName());
            addStatement.setString(2, user.getTag());
            addStatement.setString(3, user.getPublicRsa());
            addStatement.setString(4, user.getPhoto());
            addStatement.executeUpdate();
            addStatement.getGeneratedKeys();
            ResultSet resultSet = addStatement.getGeneratedKeys();
            if (resultSet.next())
                user.setId(resultSet.getLong(1));
        } catch (SQLIntegrityConstraintViolationException e) {
            status = Status.TAG_IS_TAKEN;
            user.setId(-1);
            e.printStackTrace();
        } catch (SQLException e) {
            status = Status.FAIL;
            user.setId(-1);
            e.printStackTrace();
        }
    }
}
