package model;

import jakarta.persistence.*;

@Entity
@Table(name = "organization_owns")
public class OrganizationOwn {
    @Id
    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "organization_instance_id", nullable = false, length = 100)
    private String organizationInstanceId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "organization_instance_id", nullable = false)
    private Organization organization;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "entity_instance_id", nullable = false, length = 100)
    private String entityInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "resource_entity", nullable = false, length = 100)
    private String resourceEntity;

    public String getOrganizationInstanceId() {
        return organizationInstanceId;
    }

    public void setOrganizationInstanceId(String organizationInstanceId) {
        this.organizationInstanceId = organizationInstanceId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getEntityInstanceId() {
        return entityInstanceId;
    }

    public void setEntityInstanceId(String entityInstanceId) {
        this.entityInstanceId = entityInstanceId;
    }

    public String getResourceEntity() {
        return resourceEntity;
    }

    public void setResourceEntity(String resourceEntity) {
        this.resourceEntity = resourceEntity;
    }

}