package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "dataproduct_temporal")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class DataproductTemporal {
    @EmbeddedId
    private DataproductTemporalId id;

    @MapsId("dataproductInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataproduct_instance_id", nullable = false)
    private Dataproduct dataproductInstance;

    @MapsId("temporalInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temporal_instance_id", nullable = false)
    private model.Temporal temporalInstance;

    public DataproductTemporalId getId() {
        return id;
    }

    public void setId(DataproductTemporalId id) {
        this.id = id;
    }

    public Dataproduct getDataproductInstance() {
        return dataproductInstance;
    }

    public void setDataproductInstance(Dataproduct dataproductInstance) {
        this.dataproductInstance = dataproductInstance;
    }

    public model.Temporal getTemporalInstance() {
        return temporalInstance;
    }

    public void setTemporalInstance(model.Temporal temporalInstance) {
        this.temporalInstance = temporalInstance;
    }

}