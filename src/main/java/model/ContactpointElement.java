package model;

import jakarta.persistence.*;

@Entity
@Table(name = "contactpoint_element")
public class ContactpointElement {
    @EmbeddedId
    private ContactpointElementId id;

    @MapsId("contactpointInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "contactpoint_instance_id", nullable = false)
    private Contactpoint contactpointInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private model.Element elementInstance;

    public ContactpointElementId getId() {
        return id;
    }

    public void setId(ContactpointElementId id) {
        this.id = id;
    }

    public Contactpoint getContactpointInstance() {
        return contactpointInstance;
    }

    public void setContactpointInstance(Contactpoint contactpointInstance) {
        this.contactpointInstance = contactpointInstance;
    }

    public model.Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(model.Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}