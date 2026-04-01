package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwaresourcecode_contributor", schema = "metadata_catalogue")
public class SoftwaresourcecodeContributor {
    @EmbeddedId
    private SoftwaresourcecodeContributorId id;

    @MapsId("softwaresourcecodeInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "softwaresourcecode_instance_id", nullable = false)
    private Softwaresourcecode softwaresourcecode;

    public SoftwaresourcecodeContributor() {
        this.id = new SoftwaresourcecodeContributorId();
    }

    public SoftwaresourcecodeContributorId getId() { return id; }
    public void setId(SoftwaresourcecodeContributorId id) { this.id = id; }

    public String getSoftwaresourcecodeInstanceId() {
        return id != null ? id.getSoftwaresourcecodeInstanceId() : null;
    }
    public void setSoftwaresourcecodeInstanceId(String s) {
        if (id == null) id = new SoftwaresourcecodeContributorId();
        id.setSoftwaresourcecodeInstanceId(s);
    }

    public Softwaresourcecode getSoftwaresourcecode() { return softwaresourcecode; }
    public void setSoftwaresourcecode(Softwaresourcecode s) { this.softwaresourcecode = s; }

    public String getEntityInstanceId() {
        return id != null ? id.getEntityInstanceId() : null;
    }
    public void setEntityInstanceId(String s) {
        if (id == null) id = new SoftwaresourcecodeContributorId();
        id.setEntityInstanceId(s);
    }

    public String getResourceEntity() {
        return id != null ? id.getResourceEntity() : null;
    }
    public void setResourceEntity(String s) {
        if (id == null) id = new SoftwaresourcecodeContributorId();
        id.setResourceEntity(s);
    }
}
