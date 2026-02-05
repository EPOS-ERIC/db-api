package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class OrganizationMemberofId implements java.io.Serializable {
    private static final long serialVersionUID = 2049308077367948941L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "organization1_instance_id", nullable = false, length = 100)
    private String organization1InstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "organization2_instance_id", nullable = false, length = 100)
    private String organization2InstanceId;

    public String getOrganization1InstanceId() {
        return organization1InstanceId;
    }

    public void setOrganization1InstanceId(String organization1InstanceId) {
        this.organization1InstanceId = organization1InstanceId;
    }

    public String getOrganization2InstanceId() {
        return organization2InstanceId;
    }

    public void setOrganization2InstanceId(String organization2InstanceId) {
        this.organization2InstanceId = organization2InstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        OrganizationMemberofId entity = (OrganizationMemberofId) o;
        return Objects.equals(this.organization2InstanceId, entity.organization2InstanceId) &&
                Objects.equals(this.organization1InstanceId, entity.organization1InstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organization2InstanceId, organization1InstanceId);
    }

}