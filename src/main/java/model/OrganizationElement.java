package model;

import jakarta.persistence.*;

@Entity
@Table(name = "organization_element", schema = "metadata_catalogue")
public class OrganizationElement {
    @EmbeddedId
    private OrganizationElementId id;

    @MapsId("organizationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_instance_id", nullable = false)
    private Organization organizationInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private Element elementInstance;

    public OrganizationElementId getId() {
        return id;
    }

    public void setId(OrganizationElementId id) {
        this.id = id;
    }

    public Organization getOrganizationInstance() {
        return organizationInstance;
    }

    public void setOrganizationInstance(Organization organizationInstance) {
        this.organizationInstance = organizationInstance;
    }

    public Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}