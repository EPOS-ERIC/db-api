package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwareapplication_element", schema = "metadata_catalogue")
public class SoftwareapplicationElement {
    @EmbeddedId
    private SoftwareapplicationElementId id;

    @MapsId("softwareapplicationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "softwareapplication_instance_id", nullable = false)
    private Softwareapplication softwareapplicationInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private Element elementInstance;

    public SoftwareapplicationElementId getId() {
        return id;
    }

    public void setId(SoftwareapplicationElementId id) {
        this.id = id;
    }

    public Softwareapplication getSoftwareapplicationInstance() {
        return softwareapplicationInstance;
    }

    public void setSoftwareapplicationInstance(Softwareapplication softwareapplicationInstance) {
        this.softwareapplicationInstance = softwareapplicationInstance;
    }

    public Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}