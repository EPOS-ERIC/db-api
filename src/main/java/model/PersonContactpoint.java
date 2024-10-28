package model;

import jakarta.persistence.*;

@Entity
@Table(name = "person_contactpoint")
public class PersonContactpoint {
    @EmbeddedId
    private PersonContactpointId id;

    @MapsId("personInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "person_instance_id", nullable = false)
    private Person personInstance;

    @MapsId("contactpointInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "contactpoint_instance_id", nullable = false)
    private Contactpoint contactpointInstance;

    public PersonContactpointId getId() {
        return id;
    }

    public void setId(PersonContactpointId id) {
        this.id = id;
    }

    public Person getPersonInstance() {
        return personInstance;
    }

    public void setPersonInstance(Person personInstance) {
        this.personInstance = personInstance;
    }

    public Contactpoint getContactpointInstance() {
        return contactpointInstance;
    }

    public void setContactpointInstance(Contactpoint contactpointInstance) {
        this.contactpointInstance = contactpointInstance;
    }

}