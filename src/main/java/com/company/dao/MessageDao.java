package com.company.dao;

import com.company.dao.entities.Message;

import java.sql.SQLException;
import java.util.List;

public interface MessageDao {

    void addMessage(Message message) throws SQLException;

    List<Message> getMessagesForReceiverById(long toId);

    void deleteMessagesByReceiverId(long toId);
}
