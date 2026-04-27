package com.library.client;

import com.library.client.network.ClientNetworkManager;
import com.library.common.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class LibraryApp extends Application {
    private Stage primaryStage;
    private User currentUser;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Library Management System");
        showLoginScreen();
    }

    private void showLoginScreen() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(40));
        layout.setAlignment(Pos.CENTER);
        layout.getStyleClass().add("root");

        Label title = new Label("Library Login");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        nameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginBtn = new Button("Login");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> {
            String name = nameField.getText();
            String pass = passwordField.getText();
            Request req = new Request("LOGIN", new User(0, name, pass, ""));
            Response res = ClientNetworkManager.getInstance().sendRequest(req);

            if (res != null && res.getData() instanceof User) {
                currentUser = (User) res.getData();
                if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
                    showAdminDashboard();
                } else {
                    showUserDashboard();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid credentials");
                alert.show();
            }
        });

        layout.getChildren().addAll(title, nameField, passwordField, loginBtn);

        Scene scene = new Scene(layout, 400, 500);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showAdminDashboard() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("root");

        HBox header = new HBox(10);
        Label title = new Label("Admin Dashboard - Welcome " + currentUser.getName());
        title.setStyle("-fx-font-size: 18px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TableView<Book> table = createBookTable();

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshAllBooks(table));

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> showLoginScreen());
        header.getChildren().addAll(title, spacer, refreshBtn, logoutBtn);

        refreshAllBooks(table);

        HBox controls = new HBox(10);
        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        TextField authorField = new TextField();
        authorField.setPromptText("Author");
        Button addBtn = new Button("Add Book");
        addBtn.setOnAction(e -> {
            Book b = new Book(0, titleField.getText(), authorField.getText(), true);
            ClientNetworkManager.getInstance().sendRequest(new Request("ADD_BOOK", b));
            refreshAllBooks(table);
            titleField.clear();
            authorField.clear();
        });

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.setOnAction(e -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ClientNetworkManager.getInstance().sendRequest(new Request("DELETE_BOOK", selected.getId()));
                refreshAllBooks(table);
            }
        });

        controls.getChildren().addAll(titleField, authorField, addBtn, deleteBtn);

        layout.getChildren().addAll(header, table, controls);

        Scene scene = new Scene(layout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    private void showUserDashboard() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("root");

        HBox header = new HBox(10);
        Label title = new Label("User Dashboard - Welcome " + currentUser.getName());
        title.setStyle("-fx-font-size: 18px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TableView<Book> allBooksTable = createBookTable();
        TableView<Book> myBooksTable = createBookTable();

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> {
            refreshAllBooks(allBooksTable);
            refreshMyBooks(myBooksTable);
        });

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> showLoginScreen());
        header.getChildren().addAll(title, spacer, refreshBtn, logoutBtn);

        VBox allSection = new VBox(5, new Label("Available Library Books:"), allBooksTable);
        VBox mySection = new VBox(5, new Label("My Borrowed Books (Personal Table):"), myBooksTable);

        Button borrowBtn = new Button("Borrow Selected");
        borrowBtn.setOnAction(e -> {
            Book selected = allBooksTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.isAvailable()) {
                BorrowRecord record = new BorrowRecord(0, currentUser.getId(), selected.getId(), null);
                Response response = ClientNetworkManager.getInstance().sendRequest(new Request("BORROW_BOOK", record));

                if (response != null && "ALREADY_BORROWED".equals(response.getData())) {
                    new Alert(Alert.AlertType.ERROR, "Sorry, this book was just taken by someone else!").show();
                }

                refreshAllBooks(allBooksTable);
                refreshMyBooks(myBooksTable);
            }
        });

        Button returnBtn = new Button("Return Selected");
        returnBtn.setOnAction(e -> {
            Book selected = myBooksTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                int[] data = { currentUser.getId(), selected.getId() };
                ClientNetworkManager.getInstance().sendRequest(new Request("RETURN_BOOK", data));
                refreshAllBooks(allBooksTable);
                refreshMyBooks(myBooksTable);
            }
        });

        HBox controls = new HBox(10, borrowBtn, returnBtn);
        layout.getChildren().addAll(header, allSection, mySection, controls);

        refreshAllBooks(allBooksTable);
        refreshMyBooks(myBooksTable);

        Scene scene = new Scene(layout, 900, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    private TableView<Book> createBookTable() {
        TableView<Book> table = new TableView<>();
        TableColumn<Book, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Book, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        TableColumn<Book, Boolean> availCol = new TableColumn<>("Available");
        availCol.setCellValueFactory(new PropertyValueFactory<>("available"));

        table.getColumns().addAll(idCol, titleCol, authorCol, availCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(250);
        return table;
    }

    @SuppressWarnings("unchecked")
    private void refreshAllBooks(TableView<Book> table) {
        Response res = ClientNetworkManager.getInstance().sendRequest(new Request("GET_BOOKS", null));
        if (res != null && res.getData() instanceof List) {
            table.setItems(FXCollections.observableArrayList((List<Book>) res.getData()));
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshMyBooks(TableView<Book> table) {
        Response res = ClientNetworkManager.getInstance().sendRequest(new Request("GET_MY_BOOKS", currentUser.getId()));
        if (res != null && res.getData() instanceof List) {
            table.setItems(FXCollections.observableArrayList((List<Book>) res.getData()));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
