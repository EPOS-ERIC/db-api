package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "service_temporal")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class ServiceTemporal {
    @EmbeddedId
    private ServiceTemporalId id;

    @MapsId("serviceInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_instance_id", nullable = false)
    private Service serviceInstance;

    @MapsId("temporalInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temporal_instance_id", nullable = false)
    private model.Temporal temporalInstance;

    public ServiceTemporalId getId() {
        return id;
    }

    public void setId(ServiceTemporalId id) {
        this.id = id;
    }

    public Service getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(Service serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public model.Temporal getTemporalInstance() {
        return temporalInstance;
    }

    public void setTemporalInstance(model.Temporal temporalInstance) {
        this.temporalInstance = temporalInstance;
    }

}