package model;

import jakarta.persistence.*;

@Entity
@Table(name = "service_spatial")
public class ServiceSpatial {
    @EmbeddedId
    private ServiceSpatialId id;

    @MapsId("serviceInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "service_instance_id", nullable = false)
    private Service serviceInstance;

    @MapsId("spatialInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "spatial_instance_id", nullable = false)
    private model.Spatial spatialInstance;

    public ServiceSpatialId getId() {
        return id;
    }

    public void setId(ServiceSpatialId id) {
        this.id = id;
    }

    public Service getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(Service serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public model.Spatial getSpatialInstance() {
        return spatialInstance;
    }

    public void setSpatialInstance(model.Spatial spatialInstance) {
        this.spatialInstance = spatialInstance;
    }

}