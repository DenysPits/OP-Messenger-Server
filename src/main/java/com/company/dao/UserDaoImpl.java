package com.company.dao;

import com.company.Server;
import com.company.dao.entities.Message;
import com.company.dao.entities.User;

import java.sql.*;

public class UserDaoImpl implements UserDao {
    @Override
    public void addUser(User user) throws SQLException {
        String add = "INSERT INTO users (name, tag, photo) VALUES (?, ?, ?);";
        Connection connection = Server.getConnection();
        try (PreparedStatement addStatement = connection.prepareStatement(add, Statement.RETURN_GENERATED_KEYS)) {
            addStatement.setString(1, user.getName());
            addStatement.setString(2, user.getTag());
            addStatement.setString(3, user.getPhoto());
            addStatement.executeUpdate();
            addStatement.getGeneratedKeys();
            ResultSet resultSet = addStatement.getGeneratedKeys();
            if (resultSet.next())
                user.setId(resultSet.getLong(1));
        }
    }

    @Override
    public void updateUser(User user) throws SQLException {
        String update = "UPDATE users SET name=?, tag=?, photo=? WHERE id=?";
        Connection connection = Server.getConnection();
        try (PreparedStatement updateStatement = connection.prepareStatement(update)) {
            updateStatement.setString(1, user.getName());
            updateStatement.setString(2, user.getTag());
            updateStatement.setString(3, user.getPhoto());
            updateStatement.setLong(4, user.getId());
            updateStatement.executeUpdate();
        }
    }

    @Override
    public User getUserById(long id) {
        String getById = "SELECT * FROM users WHERE id=?";
        Connection connection = Server.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(getById)) {
            preparedStatement.setString(1, String.valueOf(id));
            return makeUserFromResultSet(preparedStatement);
        } catch (SQLException throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    @Override
    public User getUserByTag(String tag) {
        String getByTag = "SELECT * FROM users WHERE tag=?";
        Connection connection = Server.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(getByTag)) {
            preparedStatement.setString(1, tag);
            return makeUserFromResultSet(preparedStatement);
        } catch (SQLException throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    @Override
    public void addUsersRelationship(Message message) {
        String addRelationshipForSender =
                "UPDATE users SET relationships=CONCAT(IFNULL(relationships,''),'" + message.getToId() + " ') WHERE " +
                        "id=" + message.getFromId();
        String addRelationshipForReceiver =
                "UPDATE users SET relationships=CONCAT(IFNULL(relationships,''),'" + message.getFromId() + " ') WHERE " +
                        "id=" + message.getToId();
        Connection connection = Server.getConnection();
        try (Statement addRelationshipStatement = connection.createStatement()) {
            String userRelationships = getUserRelationshipsById(message.getFromId());
            if (!checkSameRelationships(userRelationships, message.getToId())) {
                addRelationshipStatement.executeUpdate(addRelationshipForSender);
                addRelationshipStatement.executeUpdate(addRelationshipForReceiver);
            }
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public String getUserRelationshipsById(long id) {
        String selectRelationships = "SELECT relationships FROM users WHERE id=?";
        Connection connection = Server.getConnection();
        try (PreparedStatement selectRelationshipsStatement = connection.prepareStatement(selectRelationships)) {
            selectRelationshipsStatement.setLong(1, id);
            ResultSet relationsSet = selectRelationshipsStatement.executeQuery();
            if (relationsSet.next()) {
                return relationsSet.getString(1);
            }
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }
        return null;
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
                user.setTag(userResultSet.getString("tag"));
            }
            return user;
        } catch (SQLException throwable) {
            throwable.printStackTrace();
            return null;
        }
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
}
