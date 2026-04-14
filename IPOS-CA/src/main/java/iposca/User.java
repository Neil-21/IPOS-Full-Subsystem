package iposca;

import javafx.beans.property.*;

public class User {
    private final StringProperty username;
    private final StringProperty role;
    private final StringProperty fullName;
    private final StringProperty active;

    public User(String username, String role, String fullName, String active) {
        this.username = new SimpleStringProperty(username);
        this.role = new SimpleStringProperty(role);
        this.fullName = new SimpleStringProperty(fullName);
        this.active = new SimpleStringProperty(active);
    }

    public String getUsername() { return username.get(); }
    public String getRole() { return role.get(); }
    public String getFullName() { return fullName.get(); }
    public String getActive() { return active.get(); }
}