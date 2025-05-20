package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "facility_spatial", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class FacilitySpatial {
    @EmbeddedId
    private FacilitySpatialId id;

    @MapsId("facilityInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_instance_id", nullable = false)
    private Facility facilityInstance;

    @MapsId("spatialInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spatial_instance_id", nullable = false)
    private model.Spatial spatialInstance;

    public FacilitySpatialId getId() {
        return id;
    }

    public void setId(FacilitySpatialId id) {
        this.id = id;
    }

    public Facility getFacilityInstance() {
        return facilityInstance;
    }

    public void setFacilityInstance(Facility facilityInstance) {
        this.facilityInstance = facilityInstance;
    }

    public model.Spatial getSpatialInstance() {
        return spatialInstance;
    }

    public void setSpatialInstance(model.Spatial spatialInstance) {
        this.spatialInstance = spatialInstance;
    }

}