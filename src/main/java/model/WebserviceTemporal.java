package model;

import jakarta.persistence.*;

@Entity
@Table(name = "webservice_temporal", schema = "metadata_catalogue")
public class WebserviceTemporal {
    @EmbeddedId
    private WebserviceTemporalId id;

    @MapsId("webserviceInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "webservice_instance_id", nullable = false)
    private Webservice webserviceInstance;

    @MapsId("temporalInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "temporal_instance_id", nullable = false)
    private Temporal temporalInstance;

    public WebserviceTemporalId getId() {
        return id;
    }

    public void setId(WebserviceTemporalId id) {
        this.id = id;
    }

    public Webservice getWebserviceInstance() {
        return webserviceInstance;
    }

    public void setWebserviceInstance(Webservice webserviceInstance) {
        this.webserviceInstance = webserviceInstance;
    }

    public Temporal getTemporalInstance() {
        return temporalInstance;
    }

    public void setTemporalInstance(Temporal temporalInstance) {
        this.temporalInstance = temporalInstance;
    }

}