package com.company.handlers;

import com.company.QueryNotFoundException;
import com.company.User;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;

public class UserHandler extends AbstractHandler {
    private static final String ADD = "INSERT INTO users (name, tag, public_rsa, photo) VALUES (?, ?, ?, ?);";
    private static final String UPDATE = "UPDATE users SET name=?, tag=?, public_rsa=?, photo=? WHERE id=?";
    private static final String GET_BY_ID = "SELECT * FROM users WHERE id=?";
    private static final String GET_BY_TAG = "SELECT * FROM users WHERE tag=?";

    public UserHandler(Connection connection) {
        super(connection);
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
            }
            else if (queryKey.equals("tag")) {
                user = getUserByTag(exchange, requestQuery);
                break;
            }
        }
        sendUserInResponse(exchange, user);
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

    private User getUserById(HttpExchange exchange, String query) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(GET_BY_ID)) {
            preparedStatement.setString(1, getQueryValue(query, "id"));
            return makeUserFromResultSet(preparedStatement);
        } catch (SQLException | QueryNotFoundException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    private User getUserByTag(HttpExchange exchange, String query) {
        try(PreparedStatement preparedStatement = connection.prepareStatement(GET_BY_TAG)) {
            preparedStatement.setString(1, getQueryValue(query, "tag"));
            return makeUserFromResultSet(preparedStatement);
        } catch (SQLException | QueryNotFoundException throwables) {
            throwables.printStackTrace();
            return null;
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
            sendStatus(exchange, isSuccessful, user != null ? user.getId() : -1);
        }
    }

    private void processUpdateStatement(User user) {
        try (PreparedStatement updateStatement = connection.prepareStatement(UPDATE)) {
            updateStatement.setString(1, user.getName());
            updateStatement.setString(2, user.getTag());
            updateStatement.setString(3, user.getPublicRsa());
            updateStatement.setString(4, user.getPhoto());
            updateStatement.setLong(5, user.getId());
            updateStatement.executeUpdate();
        } catch (SQLException throwables) {
            isSuccessful = false;
            throwables.printStackTrace();
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
        } catch (SQLException throwables) {
            isSuccessful = false;
            throwables.printStackTrace();
        }
    }
}
