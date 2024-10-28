package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class FacilityIspartofId implements java.io.Serializable {
    private static final long serialVersionUID = -3808951678332623382L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "facility1_instance_id", nullable = false, length = 100)
    private String facility1InstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "facility2_instance_id", nullable = false, length = 100)
    private String facility2InstanceId;

    public String getFacility1InstanceId() {
        return facility1InstanceId;
    }

    public void setFacility1InstanceId(String facility1InstanceId) {
        this.facility1InstanceId = facility1InstanceId;
    }

    public String getFacility2InstanceId() {
        return facility2InstanceId;
    }

    public void setFacility2InstanceId(String facility2InstanceId) {
        this.facility2InstanceId = facility2InstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        FacilityIspartofId entity = (FacilityIspartofId) o;
        return Objects.equals(this.facility2InstanceId, entity.facility2InstanceId) &&
                Objects.equals(this.facility1InstanceId, entity.facility1InstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facility2InstanceId, facility1InstanceId);
    }

}