package com.library.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static final String URL = "jdbc:mysql://localhost:3306/library_db?createDatabaseIfNotExist=true";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "password VARCHAR(100) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS books (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "title VARCHAR(200) NOT NULL, " +
                    "author VARCHAR(100) NOT NULL, " +
                    "available BOOLEAN DEFAULT TRUE)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS borrow_records (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT, " +
                    "book_id INT, " +
                    "date DATE, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id), " +
                    "FOREIGN KEY (book_id) REFERENCES books(id))");

            stmt.executeUpdate(
                    "INSERT IGNORE INTO users (id, name, password, role) VALUES (1, 'admin', 'admin', 'ADMIN')");
            stmt.executeUpdate(
                    "INSERT IGNORE INTO users (id, name, password, role) VALUES (2, 'user', 'user', 'USER')");
            stmt.executeUpdate(
                    "INSERT IGNORE INTO users (id, name, password, role) VALUES (3, 'user1', 'user1', 'USER')");
            stmt.executeUpdate(
                    "INSERT IGNORE INTO users (id, name, password, role) VALUES (4, 'user2', 'user2', 'USER')");
            stmt.executeUpdate(
                    "INSERT IGNORE INTO users (id, name, password, role) VALUES (5, 'user3', 'user3', 'USER')");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
