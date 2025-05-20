package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "organization_contactpoint", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class OrganizationContactpoint {
    @EmbeddedId
    private OrganizationContactpointId id;

    @MapsId("organizationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_instance_id", nullable = false)
    private Organization organizationInstance;

    @MapsId("contactpointInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contactpoint_instance_id", nullable = false)
    private Contactpoint contactpointInstance;

    public OrganizationContactpointId getId() {
        return id;
    }

    public void setId(OrganizationContactpointId id) {
        this.id = id;
    }

    public Organization getOrganizationInstance() {
        return organizationInstance;
    }

    public void setOrganizationInstance(Organization organizationInstance) {
        this.organizationInstance = organizationInstance;
    }

    public Contactpoint getContactpointInstance() {
        return contactpointInstance;
    }

    public void setContactpointInstance(Contactpoint contactpointInstance) {
        this.contactpointInstance = contactpointInstance;
    }

}