package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class FacilitySpatialId implements java.io.Serializable {
    private static final long serialVersionUID = 1465468859083736007L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "facility_instance_id", nullable = false, length = 100)
    private String facilityInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "spatial_instance_id", nullable = false, length = 100)
    private String spatialInstanceId;

    public String getFacilityInstanceId() {
        return facilityInstanceId;
    }

    public void setFacilityInstanceId(String facilityInstanceId) {
        this.facilityInstanceId = facilityInstanceId;
    }

    public String getSpatialInstanceId() {
        return spatialInstanceId;
    }

    public void setSpatialInstanceId(String spatialInstanceId) {
        this.spatialInstanceId = spatialInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        FacilitySpatialId entity = (FacilitySpatialId) o;
        return Objects.equals(this.facilityInstanceId, entity.facilityInstanceId) &&
                Objects.equals(this.spatialInstanceId, entity.spatialInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facilityInstanceId, spatialInstanceId);
    }

}