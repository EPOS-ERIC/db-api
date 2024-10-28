package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class FacilityElementId implements java.io.Serializable {
    private static final long serialVersionUID = 2339562080229304640L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "facility_instance_id", nullable = false, length = 100)
    private String facilityInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "element_instance_id", nullable = false, length = 100)
    private String elementInstanceId;

    public String getFacilityInstanceId() {
        return facilityInstanceId;
    }

    public void setFacilityInstanceId(String facilityInstanceId) {
        this.facilityInstanceId = facilityInstanceId;
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
        FacilityElementId entity = (FacilityElementId) o;
        return Objects.equals(this.facilityInstanceId, entity.facilityInstanceId) &&
                Objects.equals(this.elementInstanceId, entity.elementInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facilityInstanceId, elementInstanceId);
    }

}