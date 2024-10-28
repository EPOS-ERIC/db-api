package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwareapplication_category")
public class SoftwareapplicationCategory {
    @EmbeddedId
    private SoftwareapplicationCategoryId id;

    @MapsId("softwareapplicationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "softwareapplication_instance_id", nullable = false)
    private Softwareapplication softwareapplicationInstance;

    @MapsId("categoryInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "category_instance_id", nullable = false)
    private Category categoryInstance;

    public SoftwareapplicationCategoryId getId() {
        return id;
    }

    public void setId(SoftwareapplicationCategoryId id) {
        this.id = id;
    }

    public Softwareapplication getSoftwareapplicationInstance() {
        return softwareapplicationInstance;
    }

    public void setSoftwareapplicationInstance(Softwareapplication softwareapplicationInstance) {
        this.softwareapplicationInstance = softwareapplicationInstance;
    }

    public Category getCategoryInstance() {
        return categoryInstance;
    }

    public void setCategoryInstance(Category categoryInstance) {
        this.categoryInstance = categoryInstance;
    }

}