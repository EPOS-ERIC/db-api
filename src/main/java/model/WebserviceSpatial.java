package model;

import jakarta.persistence.*;

@Entity
@Table(name = "webservice_spatial", schema = "metadata_catalogue")
public class WebserviceSpatial {
    @EmbeddedId
    private WebserviceSpatialId id;

    @MapsId("webserviceInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webservice_instance_id", nullable = false)
    private Webservice webserviceInstance;

    @MapsId("spatialInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spatial_instance_id", nullable = false)
    private Spatial spatialInstance;

    public WebserviceSpatialId getId() {
        return id;
    }

    public void setId(WebserviceSpatialId id) {
        this.id = id;
    }

    public Webservice getWebserviceInstance() {
        return webserviceInstance;
    }

    public void setWebserviceInstance(Webservice webserviceInstance) {
        this.webserviceInstance = webserviceInstance;
    }

    public Spatial getSpatialInstance() {
        return spatialInstance;
    }

    public void setSpatialInstance(Spatial spatialInstance) {
        this.spatialInstance = spatialInstance;
    }

}