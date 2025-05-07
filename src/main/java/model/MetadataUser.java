package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "metadata_user")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
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

    @Override
    public String toString() {
        return "MetadataUser{" +
                "authIdentifier='" + authIdentifier + '\'' +
                ", familyname='" + familyname + '\'' +
                ", givenname='" + givenname + '\'' +
                ", email='" + email + '\'' +
                ", isadmin='" + isadmin + '\'' +
                '}';
    }
}