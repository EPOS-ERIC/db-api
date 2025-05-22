package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "category_hastopconcept", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class CategoryHastopconcept {
    @EmbeddedId
    private CategoryHastopconceptId id;

    @MapsId("categorySchemeInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_scheme_instance_id", nullable = false)
    private model.CategoryScheme categorySchemeInstance;

    @MapsId("categoryInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_instance_id", nullable = false)
    private Category categoryInstance;

    public CategoryHastopconceptId getId() {
        return id;
    }

    public void setId(CategoryHastopconceptId id) {
        this.id = id;
    }

    public model.CategoryScheme getCategorySchemeInstance() {
        return categorySchemeInstance;
    }

    public void setCategorySchemeInstance(model.CategoryScheme categorySchemeInstance) {
        this.categorySchemeInstance = categorySchemeInstance;
    }

    public Category getCategoryInstance() {
        return categoryInstance;
    }

    public void setCategoryInstance(Category categoryInstance) {
        this.categoryInstance = categoryInstance;
    }

}