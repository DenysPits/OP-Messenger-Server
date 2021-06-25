package com.company.dao;

import com.company.dao.entities.Message;
import com.company.dao.entities.User;

import java.sql.SQLException;

public interface UserDao {

    void addUser(User user) throws SQLException;

    void updateUser(User user) throws SQLException;

    User getUserById(long id);

    User getUserByTag(String tag);

    void addUsersRelationship(Message message);

    String getUserRelationshipsById(long id);
}
