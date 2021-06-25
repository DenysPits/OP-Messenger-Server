package com.company.dao;

import com.company.Server;
import com.company.dao.entities.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class MessageDaoImpl implements MessageDao {
    private final BitSet messagesInDatabase = new BitSet();

    public MessageDaoImpl() {
        populateMessagesBitSet();
    }

    @Override
    public void addMessage(Message message) throws SQLException {
        String add = "INSERT INTO messages (from_id, to_id, body, time, action) VALUES (?, ?, ?, ?, ?);";
        Connection connection = Server.getConnection();
        try (PreparedStatement addStatement =
                     connection.prepareStatement(add, Statement.RETURN_GENERATED_KEYS)) {
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

    @Override
    public List<Message> getMessagesForReceiverById(long toId) {
        String getMessagesToId = "SELECT * FROM messages WHERE to_id = ?;";
        List<Message> messages = new ArrayList<>();

        if (!areMessagesInBitset(toId)) {
            return messages;
        }

        Connection connection = Server.getConnection();
        try (PreparedStatement getStatement = connection.prepareStatement(getMessagesToId)) {
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
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }
        return messages;
    }

    @Override
    public void deleteMessagesByReceiverId(long toId) {
        if (!areMessagesInBitset(toId)) return;

        String delete = "DELETE FROM messages WHERE to_id = ?";
        Connection connection = Server.getConnection();
        try (PreparedStatement deleteStatement = connection.prepareStatement(delete)) {
            deleteStatement.setLong(1, toId);
            deleteStatement.executeUpdate();
            deleteMessageInBitset(toId);
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }
    }

    private void populateMessagesBitSet() {
        Connection connection = Server.getConnection();
        String getAll = "SELECT * FROM messages;";
        try (PreparedStatement getStatement = connection.prepareStatement(getAll)) {
            ResultSet resultSet = getStatement.executeQuery();
            while (resultSet.next()) {
                messagesInDatabase.set((int) resultSet.getLong("to_id"));
            }
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }
    }

    private void addMessageToBitset(long id) {
        messagesInDatabase.set((int) id);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean areMessagesInBitset(long id) {
        return messagesInDatabase.get((int) id);
    }

    private void deleteMessageInBitset(long id) {
        messagesInDatabase.clear((int) id);
    }
}
