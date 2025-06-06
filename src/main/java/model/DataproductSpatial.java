package model;

import jakarta.persistence.*;

@Entity
@Table(name = "dataproduct_spatial", schema = "metadata_catalogue")
public class DataproductSpatial {
    @EmbeddedId
    private DataproductSpatialId id;

    @MapsId("dataproductInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "dataproduct_instance_id", nullable = false)
    private Dataproduct dataproductInstance;

    @MapsId("spatialInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "spatial_instance_id", nullable = false)
    private model.Spatial spatialInstance;

    public DataproductSpatialId getId() {
        return id;
    }

    public void setId(DataproductSpatialId id) {
        this.id = id;
    }

    public Dataproduct getDataproductInstance() {
        return dataproductInstance;
    }

    public void setDataproductInstance(Dataproduct dataproductInstance) {
        this.dataproductInstance = dataproductInstance;
    }

    public model.Spatial getSpatialInstance() {
        return spatialInstance;
    }

    public void setSpatialInstance(model.Spatial spatialInstance) {
        this.spatialInstance = spatialInstance;
    }

}