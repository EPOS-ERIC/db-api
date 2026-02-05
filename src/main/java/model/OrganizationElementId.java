package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class OrganizationElementId implements java.io.Serializable {
    private static final long serialVersionUID = -7782221437167790546L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "organization_instance_id", nullable = false, length = 100)
    private String organizationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "element_instance_id", nullable = false, length = 100)
    private String elementInstanceId;

    public String getOrganizationInstanceId() {
        return organizationInstanceId;
    }

    public void setOrganizationInstanceId(String organizationInstanceId) {
        this.organizationInstanceId = organizationInstanceId;
    }

    public String getElementInstanceId() {
        return elementInstanceId;
    }

    public void setElementInstanceId(String elementInstanceId) {
        this.elementInstanceId = elementInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        OrganizationElementId entity = (OrganizationElementId) o;
        return Objects.equals(this.organizationInstanceId, entity.organizationInstanceId) &&
                Objects.equals(this.elementInstanceId, entity.elementInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationInstanceId, elementInstanceId);
    }

}