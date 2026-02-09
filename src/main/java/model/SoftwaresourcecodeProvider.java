package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwaresourcecode_provider", schema = "metadata_catalogue")
public class SoftwaresourcecodeProvider {
    @EmbeddedId
    private SoftwaresourcecodeProviderId id;

    @MapsId("softwaresourcecodeInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "softwaresourcecode_instance_id", nullable = false)
    private Softwaresourcecode softwaresourcecode;

    public SoftwaresourcecodeProvider() {
        this.id = new SoftwaresourcecodeProviderId();
    }

    public SoftwaresourcecodeProviderId getId() { return id; }
    public void setId(SoftwaresourcecodeProviderId id) { this.id = id; }

    public String getSoftwaresourcecodeInstanceId() {
        return id != null ? id.getSoftwaresourcecodeInstanceId() : null;
    }
    public void setSoftwaresourcecodeInstanceId(String s) {
        if (id == null) id = new SoftwaresourcecodeProviderId();
        id.setSoftwaresourcecodeInstanceId(s);
    }

    public Softwaresourcecode getSoftwaresourcecode() { return softwaresourcecode; }
    public void setSoftwaresourcecode(Softwaresourcecode s) { this.softwaresourcecode = s; }

    public String getEntityInstanceId() {
        return id != null ? id.getEntityInstanceId() : null;
    }
    public void setEntityInstanceId(String s) {
        if (id == null) id = new SoftwaresourcecodeProviderId();
        id.setEntityInstanceId(s);
    }

    public String getResourceEntity() {
        return id != null ? id.getResourceEntity() : null;
    }
    public void setResourceEntity(String s) {
        if (id == null) id = new SoftwaresourcecodeProviderId();
        id.setResourceEntity(s);
    }
}
