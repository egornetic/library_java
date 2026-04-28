package com.library.server.network;

import com.library.common.*;
import com.library.server.db.DatabaseConfig;
import com.library.server.repository.BookRepositoryImpl;
import com.library.server.repository.UserRepositoryImpl;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BookRepositoryImpl bookRepository = new BookRepositoryImpl();
    private UserRepositoryImpl userRepository = new UserRepositoryImpl();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (true) {
                Request request = (Request) in.readObject();
                if (request == null)
                    break;

                Object responseData = handleRequest(request);
                out.writeObject(new Response(responseData));
                out.flush();
            }

        } catch (Exception e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private Object handleRequest(Request request) {
        String command = request.getCommand();
        Object data = request.getData();

        switch (command) {
            case "LOGIN":
                User loginData = (User) data;
                return userRepository.login(loginData.getName(), loginData.getPassword());

            case "GET_BOOKS":
                return bookRepository.getAll();

            case "GET_MY_BOOKS":
                return getMyBooks((Integer) data);

            case "ADD_BOOK":
                bookRepository.save((Book) data);
                return "SUCCESS";

            case "DELETE_BOOK":
                bookRepository.delete((Integer) data);
                return "SUCCESS";

            case "BORROW_BOOK":
                return borrowBook((BorrowRecord) data);

            case "RETURN_BOOK":
                int[] returnData = (int[]) data;
                return returnBook(returnData[0], returnData[1]);

            default:
                return "UNKNOWN_COMMAND";
        }
    }

    private void ensureUserTableExists(int userId) throws SQLException {
        String tableName = "borrowed_user_" + userId;
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "book_id INT, " +
                    "date DATE, " +
                    "FOREIGN KEY (book_id) REFERENCES books(id))");
        }
    }

    private String borrowBook(BorrowRecord record) {
        try {
            if (!isBookAvailable(record.getBookId())) {
                return "ALREADY_BORROWED";
            }

            ensureUserTableExists(record.getUserId());
            bookRepository.updateAvailability(record.getBookId(), false);

            String tableName = "borrowed_user_" + record.getUserId();
            String query = "INSERT INTO " + tableName + " (book_id, date) VALUES (?, ?)";
            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, record.getBookId());
                pstmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
                pstmt.executeUpdate();
                return "SUCCESS";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    private boolean isBookAvailable(int bookId) {
        String query = "SELECT available FROM books WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("available");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String returnBook(int userId, int bookId) {
        try {
            String tableName = "borrowed_user_" + userId;
            bookRepository.updateAvailability(bookId, true);

            String query = "DELETE FROM " + tableName + " WHERE book_id = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, bookId);
                pstmt.executeUpdate();
                return "SUCCESS";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    private List<Book> getMyBooks(int userId) {
        List<Book> books = new ArrayList<>();
        String tableName = "borrowed_user_" + userId;
        String query = "SELECT b.* FROM books b JOIN " + tableName + " m ON b.id = m.book_id";
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getBoolean("available")));
            }
        } catch (SQLException e) {
            System.out.println("No personal table yet for user " + userId);
        }
        return books;
    }
}
