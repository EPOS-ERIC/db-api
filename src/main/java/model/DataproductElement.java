package model;

import jakarta.persistence.*;

@Entity
@Table(name = "dataproduct_element", schema = "metadata_catalogue")
public class DataproductElement {
    @EmbeddedId
    private DataproductElementId id;

    @MapsId("dataproductInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "dataproduct_instance_id", nullable = false)
    private Dataproduct dataproductInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private Element elementInstance;

    public DataproductElementId getId() {
        return id;
    }

    public void setId(DataproductElementId id) {
        this.id = id;
    }

    public Dataproduct getDataproductInstance() {
        return dataproductInstance;
    }

    public void setDataproductInstance(Dataproduct dataproductInstance) {
        this.dataproductInstance = dataproductInstance;
    }

    public Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}