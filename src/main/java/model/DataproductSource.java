package model;

import jakarta.persistence.*;

@Entity
@Table(name = "dataproduct_source", schema = "metadata_catalogue")
public class DataproductSource {
    @EmbeddedId
    private DataproductSourceId id;

    @MapsId("dataproduct1InstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "dataproduct1_instance_id", nullable = false)
    private Dataproduct dataproduct1Instance;

    @MapsId("dataproduct2InstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "dataproduct2_instance_id", nullable = false)
    private Dataproduct dataproduct2Instance;

    public DataproductSourceId getId() {
        return id;
    }

    public void setId(DataproductSourceId id) {
        this.id = id;
    }

    public Dataproduct getDataproduct1Instance() {
        return dataproduct1Instance;
    }

    public void setDataproduct1Instance(Dataproduct dataproduct1Instance) {
        this.dataproduct1Instance = dataproduct1Instance;
    }

    public Dataproduct getDataproduct2Instance() {
        return dataproduct2Instance;
    }

    public void setDataproduct2Instance(Dataproduct dataproduct2Instance) {
        this.dataproduct2Instance = dataproduct2Instance;
    }

}