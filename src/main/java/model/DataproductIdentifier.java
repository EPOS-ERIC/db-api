package model;

import jakarta.persistence.*;

@Entity
@Table(name = "dataproduct_identifier")
public class DataproductIdentifier {
    @EmbeddedId
    private DataproductIdentifierId id;

    @MapsId("dataproductInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "dataproduct_instance_id", nullable = false)
    private Dataproduct dataproductInstance;

    @MapsId("identifierInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "identifier_instance_id", nullable = false)
    private model.Identifier identifierInstance;

    public DataproductIdentifierId getId() {
        return id;
    }

    public void setId(DataproductIdentifierId id) {
        this.id = id;
    }

    public Dataproduct getDataproductInstance() {
        return dataproductInstance;
    }

    public void setDataproductInstance(Dataproduct dataproductInstance) {
        this.dataproductInstance = dataproductInstance;
    }

    public model.Identifier getIdentifierInstance() {
        return identifierInstance;
    }

    public void setIdentifierInstance(model.Identifier identifierInstance) {
        this.identifierInstance = identifierInstance;
    }

}