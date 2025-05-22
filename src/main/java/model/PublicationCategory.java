package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "publication_category", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class PublicationCategory {
    @EmbeddedId
    private PublicationCategoryId id;

    @MapsId("publicationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_instance_id", nullable = false)
    private Publication publicationInstance;

    @MapsId("categoryInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_instance_id", nullable = false)
    private Category categoryInstance;

    public PublicationCategoryId getId() {
        return id;
    }

    public void setId(PublicationCategoryId id) {
        this.id = id;
    }

    public Publication getPublicationInstance() {
        return publicationInstance;
    }

    public void setPublicationInstance(Publication publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    public Category getCategoryInstance() {
        return categoryInstance;
    }

    public void setCategoryInstance(Category categoryInstance) {
        this.categoryInstance = categoryInstance;
    }

}