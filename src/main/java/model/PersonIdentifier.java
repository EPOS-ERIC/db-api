package model;

import jakarta.persistence.*;

@Entity
@Table(name = "person_identifier", schema = "metadata_catalogue")
public class PersonIdentifier {
    @EmbeddedId
    private PersonIdentifierId id;

    @MapsId("personInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "person_instance_id", nullable = false)
    private Person personInstance;

    @MapsId("identifierInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "identifier_instance_id", nullable = false)
    private Identifier identifierInstance;

    public PersonIdentifierId getId() {
        return id;
    }

    public void setId(PersonIdentifierId id) {
        this.id = id;
    }

    public Person getPersonInstance() {
        return personInstance;
    }

    public void setPersonInstance(Person personInstance) {
        this.personInstance = personInstance;
    }

    public Identifier getIdentifierInstance() {
        return identifierInstance;
    }

    public void setIdentifierInstance(Identifier identifierInstance) {
        this.identifierInstance = identifierInstance;
    }

}