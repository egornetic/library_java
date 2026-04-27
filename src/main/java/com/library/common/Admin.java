package com.library.common;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
        setRole("ADMIN");
    }

    public Admin(int id, String name, String password) {
        super(id, name, password, "ADMIN");
    }
}
