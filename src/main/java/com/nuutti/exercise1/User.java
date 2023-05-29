package com.nuutti.exercise1;

public class User {
    private String username;
    private String email;
    private String password;
    private String salt;

    public User(String username, String email, String password, String salt) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.salt = salt;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getSalt() {
        return salt;
    }
}
