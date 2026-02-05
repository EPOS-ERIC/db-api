package org.epos.eposdatamodel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class User {

    @Schema(name="authIdentifier", description = "User identifier", example = "id234324543", required = false)
    private String authIdentifier;

    @Schema(name="lastName", description = "Lastname of the user", example = "Doe", required = false)
    private String lastName;
    @Schema(name="firstName", description = "Firstname of the user", example = "John", required = false)
    private String firstName;
    @Schema(name="email", description = "Email of the user", example = "email@email.com", required = false)
    private String email;

    @Schema(name="isAdmin", description = "Boolean, true if the user is a general admin", example = "true", required = false)
    private Boolean isAdmin;

    @Schema(name="groups", description = "List of groups on which user belong to", required = false)
    private List<UserGroup> groups;

    public User(){}

    public User(String authIdentifier, String familyname, String firstName, String email, Boolean isAdmin) {
        this.authIdentifier = authIdentifier;
        this.lastName = familyname;
        this.firstName = firstName;
        this.email = email;
        this.isAdmin = isAdmin;
        this.groups = new ArrayList<>();
    }

    public User(String authIdentifier, String familyname, String firstName, String email, String isAdmin) {
        this.authIdentifier = authIdentifier;
        this.lastName = familyname;
        this.firstName = firstName;
        this.email = email;
        this.isAdmin = Boolean.getBoolean(isAdmin);
        this.groups = new ArrayList<>();
    }

    public String getAuthIdentifier() {
        return authIdentifier;
    }

    public void setAuthIdentifier(String authIdentifier) {
        this.authIdentifier = authIdentifier;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public List<UserGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<UserGroup> groups) { this.groups = groups; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(authIdentifier, user.authIdentifier) && Objects.equals(lastName, user.lastName) && Objects.equals(firstName, user.firstName) && Objects.equals(email, user.email) && Objects.equals(isAdmin, user.isAdmin) && Objects.equals(groups, user.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authIdentifier, lastName, firstName, email, isAdmin, groups);
    }

    @Override
    public String toString() {
        return "User{" +
                "authIdentifier='" + authIdentifier + '\'' +
                ", lastName='" + lastName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", email='" + email + '\'' +
                ", isAdmin=" + isAdmin +
                ", groups=" + groups +
                '}';
    }
}
