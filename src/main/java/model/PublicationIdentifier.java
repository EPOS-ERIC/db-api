package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "publication_identifier", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class PublicationIdentifier {
    @EmbeddedId
    private PublicationIdentifierId id;

    @MapsId("publicationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_instance_id", nullable = false)
    private Publication publicationInstance;

    @MapsId("identifierInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identifier_instance_id", nullable = false)
    private Identifier identifierInstance;

    public PublicationIdentifierId getId() {
        return id;
    }

    public void setId(PublicationIdentifierId id) {
        this.id = id;
    }

    public Publication getPublicationInstance() {
        return publicationInstance;
    }

    public void setPublicationInstance(Publication publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    public Identifier getIdentifierInstance() {
        return identifierInstance;
    }

    public void setIdentifierInstance(Identifier identifierInstance) {
        this.identifierInstance = identifierInstance;
    }

}