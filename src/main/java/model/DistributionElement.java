package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "distribution_element", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class DistributionElement {
    @EmbeddedId
    private DistributionElementId id;

    @MapsId("distributionInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_instance_id", nullable = false)
    private Distribution distributionInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private model.Element elementInstance;

    public DistributionElementId getId() {
        return id;
    }

    public void setId(DistributionElementId id) {
        this.id = id;
    }

    public Distribution getDistributionInstance() {
        return distributionInstance;
    }

    public void setDistributionInstance(Distribution distributionInstance) {
        this.distributionInstance = distributionInstance;
    }

    public model.Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(model.Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}