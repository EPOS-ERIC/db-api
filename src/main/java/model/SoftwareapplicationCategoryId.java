package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class SoftwareapplicationCategoryId implements java.io.Serializable {
    private static final long serialVersionUID = -174367856975271903L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "softwareapplication_instance_id", nullable = false, length = 100)
    private String softwareapplicationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category_instance_id", nullable = false, length = 100)
    private String categoryInstanceId;

    public String getSoftwareapplicationInstanceId() {
        return softwareapplicationInstanceId;
    }

    public void setSoftwareapplicationInstanceId(String softwareapplicationInstanceId) {
        this.softwareapplicationInstanceId = softwareapplicationInstanceId;
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
        SoftwareapplicationCategoryId entity = (SoftwareapplicationCategoryId) o;
        return Objects.equals(this.softwareapplicationInstanceId, entity.softwareapplicationInstanceId) &&
                Objects.equals(this.categoryInstanceId, entity.categoryInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwareapplicationInstanceId, categoryInstanceId);
    }

}