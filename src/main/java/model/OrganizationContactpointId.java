package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class OrganizationContactpointId implements java.io.Serializable {
    private static final long serialVersionUID = 5387376707897517450L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "organization_instance_id", nullable = false, length = 100)
    private String organizationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "contactpoint_instance_id", nullable = false, length = 100)
    private String contactpointInstanceId;

    public String getOrganizationInstanceId() {
        return organizationInstanceId;
    }

    public void setOrganizationInstanceId(String organizationInstanceId) {
        this.organizationInstanceId = organizationInstanceId;
    }

    public String getContactpointInstanceId() {
        return contactpointInstanceId;
    }

    public void setContactpointInstanceId(String contactpointInstanceId) {
        this.contactpointInstanceId = contactpointInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        OrganizationContactpointId entity = (OrganizationContactpointId) o;
        return Objects.equals(this.organizationInstanceId, entity.organizationInstanceId) &&
                Objects.equals(this.contactpointInstanceId, entity.contactpointInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationInstanceId, contactpointInstanceId);
    }

}