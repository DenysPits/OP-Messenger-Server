package com.company;

import com.company.handlers.MessageHandler;
import com.company.handlers.UserHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.*;

public class Server {
    public static final String url = "jdbc:mysql://localhost:3306/messenger_database";
    public static final String username = "user";
    public static final String password = "root";
    
    public static void main(String[] args) throws IOException {
        try {
            Driver driver = new com.mysql.cj.jdbc.Driver();
            DriverManager.registerDriver(driver);
        } catch (SQLException throwables) {
            System.out.println("Problems with driver");
        }

        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            HttpServer server = HttpServer.create();
            server.bind(new InetSocketAddress(8000), 0);
            System.out.println("Server was bind");
            server.createContext("/api/messages/", new MessageHandler(connection));
            server.createContext("/api/users/", new UserHandler(connection));
            server.setExecutor(null);
            server.start();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}