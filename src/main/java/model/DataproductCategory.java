package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "dataproduct_category", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class DataproductCategory {
    @EmbeddedId
    private DataproductCategoryId id;

    @MapsId("dataproductInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataproduct_instance_id", nullable = false)
    private Dataproduct dataproductInstance;

    @MapsId("categoryInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_instance_id", nullable = false)
    private Category categoryInstance;

    public DataproductCategoryId getId() {
        return id;
    }

    public void setId(DataproductCategoryId id) {
        this.id = id;
    }

    public Dataproduct getDataproductInstance() {
        return dataproductInstance;
    }

    public void setDataproductInstance(Dataproduct dataproductInstance) {
        this.dataproductInstance = dataproductInstance;
    }

    public Category getCategoryInstance() {
        return categoryInstance;
    }

    public void setCategoryInstance(Category categoryInstance) {
        this.categoryInstance = categoryInstance;
    }

}