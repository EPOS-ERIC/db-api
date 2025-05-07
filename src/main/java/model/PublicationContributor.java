package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "publication_contributor")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class PublicationContributor {
    @EmbeddedId
    private PublicationContributorId id;

    @MapsId("publicationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_instance_id", nullable = false)
    private Publication publicationInstance;

    @MapsId("personInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_instance_id", nullable = false)
    private Person personInstance;

    public PublicationContributorId getId() {
        return id;
    }

    public void setId(PublicationContributorId id) {
        this.id = id;
    }

    public Publication getPublicationInstance() {
        return publicationInstance;
    }

    public void setPublicationInstance(Publication publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    public Person getPersonInstance() {
        return personInstance;
    }

    public void setPersonInstance(Person personInstance) {
        this.personInstance = personInstance;
    }

}