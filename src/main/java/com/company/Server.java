package com.company;

import com.company.handlers.MessageHandler;
import com.company.handlers.UserHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.*;

public class Server {
    private static String url;
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";
    private static String address;

    public static void initServerData() {
        url = "jdbc:mysql://db:3306/messenger_database?autoReconnect=true&useUnicode=true&characterEncoding=utf8";
        address = "0.0.0.0";
        /*try (BufferedReader reader = new BufferedReader(new FileReader("/app/src/main/resources/data.txt"))) {
            url = reader.readLine();
            address = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public static void initFakeServerData() {
        url = "jdbc:mysql://localhost:3306/messenger_database?autoReconnect=true&useSSL=false&useUnicode=true&characterEncoding=utf8";
        address = "localhost";
    }
    
    public static void main(String[] args) {
        try {
            initServerData();
            //initFakeServerData();
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
            Connection connection = DriverManager.getConnection(url, USERNAME, PASSWORD);
            HttpServer server = HttpServer.create(new InetSocketAddress(address, 8000), 0);
            System.out.println("Server was bind");
            server.createContext("/api/messages", MessageHandler.getInstance(connection));
            server.createContext("/api/users", UserHandler.getInstance(connection));
            server.setExecutor(null);
            server.start();
        } catch (SQLException | IOException throwables) {
            throwables.printStackTrace();
        }
    }
}