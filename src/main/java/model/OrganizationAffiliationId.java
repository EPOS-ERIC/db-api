package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class OrganizationAffiliationId implements java.io.Serializable {
    private static final long serialVersionUID = 6326895587900906683L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "person_instance_id", nullable = false, length = 100)
    private String personInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "organization_instance_id", nullable = false, length = 100)
    private String organizationInstanceId;

    public String getPersonInstanceId() {
        return personInstanceId;
    }

    public void setPersonInstanceId(String personInstanceId) {
        this.personInstanceId = personInstanceId;
    }

    public String getOrganizationInstanceId() {
        return organizationInstanceId;
    }

    public void setOrganizationInstanceId(String organizationInstanceId) {
        this.organizationInstanceId = organizationInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        OrganizationAffiliationId entity = (OrganizationAffiliationId) o;
        return Objects.equals(this.organizationInstanceId, entity.organizationInstanceId) &&
                Objects.equals(this.personInstanceId, entity.personInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationInstanceId, personInstanceId);
    }

}