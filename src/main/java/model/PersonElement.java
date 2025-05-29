package model;

import jakarta.persistence.*;

@Entity
@Table(name = "person_element", schema = "metadata_catalogue")
public class PersonElement {
    @EmbeddedId
    private PersonElementId id;

    @MapsId("personInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_instance_id", nullable = false)
    private Person personInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private Element elementInstance;

    public PersonElementId getId() {
        return id;
    }

    public void setId(PersonElementId id) {
        this.id = id;
    }

    public Person getPersonInstance() {
        return personInstance;
    }

    public void setPersonInstance(Person personInstance) {
        this.personInstance = personInstance;
    }

    public Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}