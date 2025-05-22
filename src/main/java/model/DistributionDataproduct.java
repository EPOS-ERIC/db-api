package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "distribution_dataproduct", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class DistributionDataproduct {
    @EmbeddedId
    private DistributionDataproductId id;

    @MapsId("dataproductInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataproduct_instance_id", nullable = false)
    private Dataproduct dataproductInstance;

    @MapsId("distributionInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_instance_id", nullable = false)
    private Distribution distributionInstance;

    public DistributionDataproductId getId() {
        return id;
    }

    public void setId(DistributionDataproductId id) {
        this.id = id;
    }

    public Dataproduct getDataproductInstance() {
        return dataproductInstance;
    }

    public void setDataproductInstance(Dataproduct dataproductInstance) {
        this.dataproductInstance = dataproductInstance;
    }

    public Distribution getDistributionInstance() {
        return distributionInstance;
    }

    public void setDistributionInstance(Distribution distributionInstance) {
        this.distributionInstance = distributionInstance;
    }

}