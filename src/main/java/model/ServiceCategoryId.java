package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class ServiceCategoryId implements java.io.Serializable {
    private static final long serialVersionUID = 4431238092497830626L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "service_instance_id", nullable = false, length = 100)
    private String serviceInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category_instance_id", nullable = false, length = 100)
    private String categoryInstanceId;

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
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
        ServiceCategoryId entity = (ServiceCategoryId) o;
        return Objects.equals(this.categoryInstanceId, entity.categoryInstanceId) &&
                Objects.equals(this.serviceInstanceId, entity.serviceInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryInstanceId, serviceInstanceId);
    }

}