package com.library.common;

import java.io.Serializable;
import java.time.LocalDate;

public class BorrowRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private int bookId;
    private LocalDate date;

    public BorrowRecord() {}

    public BorrowRecord(int id, int userId, int bookId, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.date = date;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
