package model;

import jakarta.persistence.*;

@Entity
@Table(name = "metadata_group_user")
public class MetadataGroupUser {
    @Id
    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @jakarta.validation.constraints.NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_identifier", nullable = false)
    private model.MetadataUser authIdentifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private MetadataGroup group;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "request_status", length = 100)
    private String requestStatus;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "role", length = 100)
    private String role;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public model.MetadataUser getAuthIdentifier() {
        return authIdentifier;
    }

    public void setAuthIdentifier(model.MetadataUser authIdentifier) {
        this.authIdentifier = authIdentifier;
    }

    public MetadataGroup getGroup() {
        return group;
    }

    public void setGroup(MetadataGroup group) {
        this.group = group;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "MetadataGroupUser{" +
                "id='" + id + '\'' +
                ", authIdentifier=" + authIdentifier +
                ", group=" + group +
                ", requestStatus='" + requestStatus + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}