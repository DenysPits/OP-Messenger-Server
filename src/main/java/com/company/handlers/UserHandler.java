package com.company.handlers;

import com.company.QueryNotFoundException;
import com.company.User;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;

public class UserHandler extends AbstractHandler {
    private static final String ADD = "INSERT INTO users (name, tag, public_rsa, photo) VALUES (?, ?, ?, ?);";
    private static final String UPDATE = "UPDATE users SET name=?, tag=?, public_rsa=?, photo=? WHERE id=?";

    public UserHandler(Connection connection) {
        super(connection);
    }

    @Override
    public void processGetRequest(HttpExchange exchange) {

    }

    @Override
    public void processPostRequest(HttpExchange exchange) throws IOException {
        User user = null;
        try (InputStream inputStream = exchange.getRequestBody();) {
            String requestQuery = exchange.getRequestURI().getQuery();
            user = objectMapper.readValue(inputStream, User.class);
            id = Long.parseLong(getQueryValue(requestQuery, "update"));
            processUpdateStatement(user);
        } catch (QueryNotFoundException queryNotFoundException) {
            processAddStatement(user);
        } finally {
            sendStatus(exchange, isSuccessful, id);
        }
    }

    private void processUpdateStatement(User user) {
        try (PreparedStatement updateStatement = connection.prepareStatement(UPDATE)) {
            updateStatement.setString(1, user.getName());
            updateStatement.setString(2, user.getTag());
            updateStatement.setString(3, user.getPublicRsa());
            updateStatement.setString(4, user.getPhoto());
            updateStatement.setLong(5, id);
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
                id = resultSet.getLong(1);
        } catch (SQLException throwables) {
            isSuccessful = false;
            throwables.printStackTrace();
        }
    }
}
