package model;

import jakarta.persistence.*;

@Entity
@Table(name = "organization_identifier", schema = "metadata_catalogue")
public class OrganizationIdentifier {
    @EmbeddedId
    private OrganizationIdentifierId id;

    @MapsId("organizationInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "organization_instance_id", nullable = false)
    private Organization organizationInstance;

    @MapsId("identifierInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "identifier_instance_id", nullable = false)
    private Identifier identifierInstance;

    public OrganizationIdentifierId getId() {
        return id;
    }

    public void setId(OrganizationIdentifierId id) {
        this.id = id;
    }

    public Organization getOrganizationInstance() {
        return organizationInstance;
    }

    public void setOrganizationInstance(Organization organizationInstance) {
        this.organizationInstance = organizationInstance;
    }

    public Identifier getIdentifierInstance() {
        return identifierInstance;
    }

    public void setIdentifierInstance(Identifier identifierInstance) {
        this.identifierInstance = identifierInstance;
    }

}