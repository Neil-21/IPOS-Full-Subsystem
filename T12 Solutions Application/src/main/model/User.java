package main.model;

public class User {
    private final String email;
    private String password;
    private final String role;
    private boolean firstLogin;
    private final String fullName;

    public User(String email, String password, String role, boolean firstLogin, String fullName) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.firstLogin = firstLogin;
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public boolean isFirstLogin() {
        return firstLogin;
    }

    public String getFullName() {
        return fullName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFirstLogin(boolean firstLogin) {
        this.firstLogin = firstLogin;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isCustomer() {
        return "CUSTOMER".equalsIgnoreCase(role);
    }
}