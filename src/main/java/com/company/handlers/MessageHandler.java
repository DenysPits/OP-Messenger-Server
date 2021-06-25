package com.company.handlers;

import com.company.dao.MessageDao;
import com.company.dao.MessageDaoImpl;
import com.company.dao.entities.Message;
import com.company.QueryNotFoundException;
import com.company.status.Status;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MessageHandler extends AbstractHandler {
    protected static Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private static MessageHandler instance;
    private final MessageDao messageDao = new MessageDaoImpl();

    private MessageHandler() {
    }

    public static MessageHandler getInstance() {
        if (instance == null) {
            synchronized (MessageHandler.class) {
                if (instance == null) {
                    instance = new MessageHandler();
                }
            }
        }
        return instance;
    }

    public MessageDao getMessageDao() {
        return messageDao;
    }

    @Override
    public void processGetRequest(HttpExchange exchange) throws IOException {
        List<Message> messages = null;
        try {
            String requestQuery = exchange.getRequestURI().getQuery();
            long toId = Long.parseLong(getQueryValue(requestQuery, "id"));
            boolean deleteMessages;
            try {
                deleteMessages = Boolean.parseBoolean(getQueryValue(requestQuery, "del"));
            } catch (QueryNotFoundException e) {
                deleteMessages = false;
            }
            messages = messageDao.getMessagesForReceiverById(toId);
            if (deleteMessages) {
                messageDao.deleteMessagesByReceiverId(toId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sendEntitiesInResponse(exchange, messages);
        }
    }

    @Override
    public void processPostRequest(HttpExchange exchange) throws IOException {
        Message message = null;
        try (InputStream inputStream = exchange.getRequestBody()) {
            message = objectMapper.readValue(inputStream, Message.class);
            messageDao.addMessage(message);
            UserHandler.getInstance().getUserDao().addUsersRelationship(message);
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
}