package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class FacilityAddressId implements java.io.Serializable {
    private static final long serialVersionUID = 3233122781869087853L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "facility_instance_id", nullable = false, length = 100)
    private String facilityInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "address_instance_id", nullable = false, length = 100)
    private String addressInstanceId;

    public String getFacilityInstanceId() {
        return facilityInstanceId;
    }

    public void setFacilityInstanceId(String facilityInstanceId) {
        this.facilityInstanceId = facilityInstanceId;
    }

    public String getAddressInstanceId() {
        return addressInstanceId;
    }

    public void setAddressInstanceId(String addressInstanceId) {
        this.addressInstanceId = addressInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        FacilityAddressId entity = (FacilityAddressId) o;
        return Objects.equals(this.facilityInstanceId, entity.facilityInstanceId) &&
                Objects.equals(this.addressInstanceId, entity.addressInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facilityInstanceId, addressInstanceId);
    }

}