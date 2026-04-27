package com.library.server;

import com.library.server.db.DatabaseConfig;
import com.library.server.network.ClientHandler;

import java.net.ServerSocket;
import java.net.Socket;

public class LibraryServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("Starting Library Server...");
        DatabaseConfig.initializeDatabase();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
