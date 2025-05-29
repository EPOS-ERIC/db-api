package model;

import jakarta.persistence.*;

@Entity
@Table(name = "service_category", schema = "metadata_catalogue")
public class ServiceCategory {
    @EmbeddedId
    private ServiceCategoryId id;

    @MapsId("serviceInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_instance_id", nullable = false)
    private Service serviceInstance;

    @MapsId("categoryInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_instance_id", nullable = false)
    private Category categoryInstance;

    public ServiceCategoryId getId() {
        return id;
    }

    public void setId(ServiceCategoryId id) {
        this.id = id;
    }

    public Service getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(Service serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public Category getCategoryInstance() {
        return categoryInstance;
    }

    public void setCategoryInstance(Category categoryInstance) {
        this.categoryInstance = categoryInstance;
    }

}