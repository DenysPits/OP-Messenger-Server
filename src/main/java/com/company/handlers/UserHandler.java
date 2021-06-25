package com.company.handlers;

import com.company.dao.UserDao;
import com.company.dao.UserDaoImpl;
import com.company.dao.entities.Message;
import com.company.QueryNotFoundException;
import com.company.status.Status;
import com.company.dao.entities.User;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;

public class UserHandler extends AbstractHandler {
    private static UserHandler instance;
    private final UserDao userDao = new UserDaoImpl();

    private UserHandler() {
    }

    public static UserHandler getInstance() {
        if (instance == null) {
            synchronized (UserHandler.class) {
                if (instance == null) {
                    instance = new UserHandler();
                }
            }
        }
        return instance;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    @Override
    public void processGetRequest(HttpExchange exchange) throws IOException {
        String requestQuery = exchange.getRequestURI().getQuery();
        String[] queryKeys = getQueryKeys(requestQuery);
        User user = null;
        try {
            for (String queryKey : queryKeys) {
                if (queryKey.equals("id")) {
                    long id = Long.parseLong(getQueryValue(requestQuery, "id"));
                    user = userDao.getUserById(id);
                    break;
                } else if (queryKey.equals("tag")) {
                    String tag = getQueryValue(requestQuery, "tag");
                    user = userDao.getUserByTag(tag);
                    break;
                }
            }
        } catch (QueryNotFoundException e) {
            e.printStackTrace();
        } finally {
            sendEntitiesInResponse(exchange, user);
        }
    }

    @Override
    public void processPostRequest(HttpExchange exchange) throws IOException {
        User user = null;
        try (InputStream inputStream = exchange.getRequestBody()) {
            String requestQuery = exchange.getRequestURI().getQuery();
            user = objectMapper.readValue(inputStream, User.class);
            if (requestQuery != null && requestQuery.contains("update=")) {
                user.setId(Long.parseLong(getQueryValue(requestQuery, "update")));
                userDao.updateUser(user);
                notifyPeople(user.getId());
            } else {
                userDao.addUser(user);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            status = Status.TAG_IS_TAKEN;
            e.printStackTrace();
        } catch (SQLException e) {
            status = Status.FAIL;
            e.printStackTrace();
        } catch (QueryNotFoundException e) {
            e.printStackTrace();
        } finally {
            sendStatus(exchange, getStatusResponse(user != null ? user.getId() : -1));
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

    private void notifyPeople(long idWhoWasUpdated) throws SQLException {
        String userRelationshipsStr = userDao.getUserRelationshipsById(idWhoWasUpdated);
        if (userRelationshipsStr == null) {
            return;
        }
        Message updateMessage = new Message();
        updateMessage.setAction("update");
        updateMessage.setFromId(0);
        updateMessage.setBody(String.valueOf(idWhoWasUpdated));
        String[] userRelationships = userRelationshipsStr.trim().split("\\s+");
        MessageHandler messageHandler = MessageHandler.getInstance();
        for (String userRelationship : userRelationships) {
            updateMessage.setToId(Long.parseLong(userRelationship));
            messageHandler.getMessageDao().addMessage(updateMessage);
        }
    }
}