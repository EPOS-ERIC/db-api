package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "metadata_user")
public class MetadataUser {
    @Id
    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "auth_identifier", nullable = false, length = 1024)
    private String authIdentifier;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "familyname", length = 1024)
    private String familyname;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "givenname", length = 1024)
    private String givenname;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "email", length = 1024)
    private String email;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "isadmin", length = 1024)
    private String isadmin;

    public String getAuthIdentifier() {
        return authIdentifier;
    }

    public void setAuthIdentifier(String authIdentifier) {
        this.authIdentifier = authIdentifier;
    }

    public String getFamilyname() {
        return familyname;
    }

    public void setFamilyname(String familyname) {
        this.familyname = familyname;
    }

    public String getGivenname() {
        return givenname;
    }

    public void setGivenname(String givenname) {
        this.givenname = givenname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIsadmin() {
        return isadmin;
    }

    public void setIsadmin(String isadmin) {
        this.isadmin = isadmin;
    }

}