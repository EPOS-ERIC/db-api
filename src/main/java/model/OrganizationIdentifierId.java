package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class OrganizationIdentifierId implements java.io.Serializable {
    private static final long serialVersionUID = 3591601296105781626L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "organization_instance_id", nullable = false, length = 100)
    private String organizationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "identifier_instance_id", nullable = false, length = 100)
    private String identifierInstanceId;

    public String getOrganizationInstanceId() {
        return organizationInstanceId;
    }

    public void setOrganizationInstanceId(String organizationInstanceId) {
        this.organizationInstanceId = organizationInstanceId;
    }

    public String getIdentifierInstanceId() {
        return identifierInstanceId;
    }

    public void setIdentifierInstanceId(String identifierInstanceId) {
        this.identifierInstanceId = identifierInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        OrganizationIdentifierId entity = (OrganizationIdentifierId) o;
        return Objects.equals(this.organizationInstanceId, entity.organizationInstanceId) &&
                Objects.equals(this.identifierInstanceId, entity.identifierInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationInstanceId, identifierInstanceId);
    }

}