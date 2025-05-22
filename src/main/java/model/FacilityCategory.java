package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "facility_category", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class FacilityCategory {
    @EmbeddedId
    private FacilityCategoryId id;

    @MapsId("facilityInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_instance_id", nullable = false)
    private Facility facilityInstance;

    @MapsId("categoryInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_instance_id", nullable = false)
    private Category categoryInstance;

    public FacilityCategoryId getId() {
        return id;
    }

    public void setId(FacilityCategoryId id) {
        this.id = id;
    }

    public Facility getFacilityInstance() {
        return facilityInstance;
    }

    public void setFacilityInstance(Facility facilityInstance) {
        this.facilityInstance = facilityInstance;
    }

    public Category getCategoryInstance() {
        return categoryInstance;
    }

    public void setCategoryInstance(Category categoryInstance) {
        this.categoryInstance = categoryInstance;
    }

}