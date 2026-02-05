package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class SoftwareapplicationContactpointId implements java.io.Serializable {
    private static final long serialVersionUID = 3072478400229465372L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "softwareapplication_instance_id", nullable = false, length = 100)
    private String softwareapplicationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "contactpoint_instance_id", nullable = false, length = 100)
    private String contactpointInstanceId;

    public String getSoftwareapplicationInstanceId() {
        return softwareapplicationInstanceId;
    }

    public void setSoftwareapplicationInstanceId(String softwareapplicationInstanceId) {
        this.softwareapplicationInstanceId = softwareapplicationInstanceId;
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
        SoftwareapplicationContactpointId entity = (SoftwareapplicationContactpointId) o;
        return Objects.equals(this.softwareapplicationInstanceId, entity.softwareapplicationInstanceId) &&
                Objects.equals(this.contactpointInstanceId, entity.contactpointInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwareapplicationInstanceId, contactpointInstanceId);
    }

}