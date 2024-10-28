package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class SoftwaresourcecodeContactpointId implements java.io.Serializable {
    private static final long serialVersionUID = 4780296148764599136L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "softwaresourcecode_instance_id", nullable = false, length = 100)
    private String softwaresourcecodeInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "contactpoint_instance_id", nullable = false, length = 100)
    private String contactpointInstanceId;

    public String getSoftwaresourcecodeInstanceId() {
        return softwaresourcecodeInstanceId;
    }

    public void setSoftwaresourcecodeInstanceId(String softwaresourcecodeInstanceId) {
        this.softwaresourcecodeInstanceId = softwaresourcecodeInstanceId;
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
        SoftwaresourcecodeContactpointId entity = (SoftwaresourcecodeContactpointId) o;
        return Objects.equals(this.softwaresourcecodeInstanceId, entity.softwaresourcecodeInstanceId) &&
                Objects.equals(this.contactpointInstanceId, entity.contactpointInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwaresourcecodeInstanceId, contactpointInstanceId);
    }

}