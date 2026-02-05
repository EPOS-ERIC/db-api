package model;

import jakarta.persistence.*;

@Entity
@Table(name = "dataproduct_attribution", schema = "metadata_catalogue")
public class DataproductAttribution {
    @EmbeddedId
    private DataproductAttributionId id;

    @MapsId("dataproductInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "dataproduct_instance_id", nullable = false)
    private Dataproduct dataproductInstance;

    @MapsId("attributionInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "attribution_instance_id", nullable = false)
    private Attribution attributionInstance;

    public DataproductAttributionId getId() {
        return id;
    }

    public void setId(DataproductAttributionId id) {
        this.id = id;
    }

    public Dataproduct getDataproductInstance() {
        return dataproductInstance;
    }

    public void setDataproductInstance(Dataproduct dataproductInstance) {
        this.dataproductInstance = dataproductInstance;
    }

    public Attribution getAttributionInstance() {
        return attributionInstance;
    }

    public void setAttributionInstance(Attribution attributionInstance) {
        this.attributionInstance = attributionInstance;
    }

}