package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class FacilityCategoryId implements java.io.Serializable {
    private static final long serialVersionUID = -1770963496636217088L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "facility_instance_id", nullable = false, length = 100)
    private String facilityInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category_instance_id", nullable = false, length = 100)
    private String categoryInstanceId;

    public String getFacilityInstanceId() {
        return facilityInstanceId;
    }

    public void setFacilityInstanceId(String facilityInstanceId) {
        this.facilityInstanceId = facilityInstanceId;
    }

    public String getCategoryInstanceId() {
        return categoryInstanceId;
    }

    public void setCategoryInstanceId(String categoryInstanceId) {
        this.categoryInstanceId = categoryInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        FacilityCategoryId entity = (FacilityCategoryId) o;
        return Objects.equals(this.facilityInstanceId, entity.facilityInstanceId) &&
                Objects.equals(this.categoryInstanceId, entity.categoryInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facilityInstanceId, categoryInstanceId);
    }

}