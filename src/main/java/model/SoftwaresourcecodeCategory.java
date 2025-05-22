package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "softwaresourcecode_category", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class SoftwaresourcecodeCategory {
    @EmbeddedId
    private SoftwaresourcecodeCategoryId id;

    @MapsId("softwaresourcecodeInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "softwaresourcecode_instance_id", nullable = false)
    private Softwaresourcecode softwaresourcecodeInstance;

    @MapsId("categoryInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_instance_id", nullable = false)
    private Category categoryInstance;

    public SoftwaresourcecodeCategoryId getId() {
        return id;
    }

    public void setId(SoftwaresourcecodeCategoryId id) {
        this.id = id;
    }

    public Softwaresourcecode getSoftwaresourcecodeInstance() {
        return softwaresourcecodeInstance;
    }

    public void setSoftwaresourcecodeInstance(Softwaresourcecode softwaresourcecodeInstance) {
        this.softwaresourcecodeInstance = softwaresourcecodeInstance;
    }

    public Category getCategoryInstance() {
        return categoryInstance;
    }

    public void setCategoryInstance(Category categoryInstance) {
        this.categoryInstance = categoryInstance;
    }

}