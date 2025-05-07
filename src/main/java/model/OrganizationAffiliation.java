package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "organization_affiliation")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class OrganizationAffiliation {
    @EmbeddedId
    private OrganizationAffiliationId id;

    @MapsId("personInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_instance_id", nullable = false)
    private model.Person personInstance;

    @MapsId("organizationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_instance_id", nullable = false)
    private Organization organizationInstance;

    public OrganizationAffiliationId getId() {
        return id;
    }

    public void setId(OrganizationAffiliationId id) {
        this.id = id;
    }

    public model.Person getPersonInstance() {
        return personInstance;
    }

    public void setPersonInstance(model.Person personInstance) {
        this.personInstance = personInstance;
    }

    public Organization getOrganizationInstance() {
        return organizationInstance;
    }

    public void setOrganizationInstance(Organization organizationInstance) {
        this.organizationInstance = organizationInstance;
    }

}