package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class ContactpointElementId implements java.io.Serializable {
    private static final long serialVersionUID = -7748757989754671056L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "contactpoint_instance_id", nullable = false, length = 100)
    private String contactpointInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "element_instance_id", nullable = false, length = 100)
    private String elementInstanceId;

    public String getContactpointInstanceId() {
        return contactpointInstanceId;
    }

    public void setContactpointInstanceId(String contactpointInstanceId) {
        this.contactpointInstanceId = contactpointInstanceId;
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
        ContactpointElementId entity = (ContactpointElementId) o;
        return Objects.equals(this.elementInstanceId, entity.elementInstanceId) &&
                Objects.equals(this.contactpointInstanceId, entity.contactpointInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementInstanceId, contactpointInstanceId);
    }

}