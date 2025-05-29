package model;

import jakarta.persistence.*;

@Entity
@Table(name = "webservice_distribution", schema = "metadata_catalogue")
public class WebserviceDistribution {
    @EmbeddedId
    private WebserviceDistributionId id;

    @MapsId("webserviceInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webservice_instance_id", nullable = false)
    private Webservice webserviceInstance;

    @MapsId("distributionInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_instance_id", nullable = false)
    private Distribution distributionInstance;

    public WebserviceDistributionId getId() {
        return id;
    }

    public void setId(WebserviceDistributionId id) {
        this.id = id;
    }

    public Webservice getWebserviceInstance() {
        return webserviceInstance;
    }

    public void setWebserviceInstance(Webservice webserviceInstance) {
        this.webserviceInstance = webserviceInstance;
    }

    public Distribution getDistributionInstance() {
        return distributionInstance;
    }

    public void setDistributionInstance(Distribution distributionInstance) {
        this.distributionInstance = distributionInstance;
    }

}