package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class FacilityContactpointId implements java.io.Serializable {
    private static final long serialVersionUID = -1164966211141803550L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "facility_instance_id", nullable = false, length = 100)
    private String facilityInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "contactpoint_instance_id", nullable = false, length = 100)
    private String contactpointInstanceId;

    public String getFacilityInstanceId() {
        return facilityInstanceId;
    }

    public void setFacilityInstanceId(String facilityInstanceId) {
        this.facilityInstanceId = facilityInstanceId;
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
        FacilityContactpointId entity = (FacilityContactpointId) o;
        return Objects.equals(this.facilityInstanceId, entity.facilityInstanceId) &&
                Objects.equals(this.contactpointInstanceId, entity.contactpointInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facilityInstanceId, contactpointInstanceId);
    }

}