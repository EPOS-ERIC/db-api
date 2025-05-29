package model;

import jakarta.persistence.*;

@Entity
@Table(name = "organization_memberof", schema = "metadata_catalogue")
public class OrganizationMemberof {
    @EmbeddedId
    private OrganizationMemberofId id;

    @MapsId("organization1InstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization1_instance_id", nullable = false)
    private Organization organization1Instance;

    @MapsId("organization2InstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization2_instance_id", nullable = false)
    private Organization organization2Instance;

    public OrganizationMemberofId getId() {
        return id;
    }

    public void setId(OrganizationMemberofId id) {
        this.id = id;
    }

    public Organization getOrganization1Instance() {
        return organization1Instance;
    }

    public void setOrganization1Instance(Organization organization1Instance) {
        this.organization1Instance = organization1Instance;
    }

    public Organization getOrganization2Instance() {
        return organization2Instance;
    }

    public void setOrganization2Instance(Organization organization2Instance) {
        this.organization2Instance = organization2Instance;
    }

}