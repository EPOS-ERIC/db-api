package model;

import jakarta.persistence.*;

@Entity
@Table(name = "dataproduct_publisher")
public class DataproductPublisher {
    @EmbeddedId
    private DataproductPublisherId id;

    @MapsId("dataproductInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataproduct_instance_id", nullable = false)
    private Dataproduct dataproductInstance;

    @MapsId("organizationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_instance_id", nullable = false)
    private model.Organization organizationInstance;

    public DataproductPublisherId getId() {
        return id;
    }

    public void setId(DataproductPublisherId id) {
        this.id = id;
    }

    public Dataproduct getDataproductInstance() {
        return dataproductInstance;
    }

    public void setDataproductInstance(Dataproduct dataproductInstance) {
        this.dataproductInstance = dataproductInstance;
    }

    public model.Organization getOrganizationInstance() {
        return organizationInstance;
    }

    public void setOrganizationInstance(model.Organization organizationInstance) {
        this.organizationInstance = organizationInstance;
    }

}