package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwaresourcecode_creator", schema = "metadata_catalogue")
public class SoftwaresourcecodeCreator {
    @EmbeddedId
    private SoftwaresourcecodeCreatorId id;

    @MapsId("softwaresourcecodeInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "softwaresourcecode_instance_id", nullable = false)
    private Softwaresourcecode softwaresourcecode;

    public SoftwaresourcecodeCreator() {
        this.id = new SoftwaresourcecodeCreatorId();
    }

    public SoftwaresourcecodeCreatorId getId() {
        return id;
    }

    public void setId(SoftwaresourcecodeCreatorId id) {
        this.id = id;
    }

    public String getSoftwaresourcecodeInstanceId() {
        return id != null ? id.getSoftwaresourcecodeInstanceId() : null;
    }

    public void setSoftwaresourcecodeInstanceId(String softwaresourcecodeInstanceId) {
        if (id == null) id = new SoftwaresourcecodeCreatorId();
        id.setSoftwaresourcecodeInstanceId(softwaresourcecodeInstanceId);
    }

    public Softwaresourcecode getSoftwaresourcecode() {
        return softwaresourcecode;
    }

    public void setSoftwaresourcecode(Softwaresourcecode softwaresourcecode) {
        this.softwaresourcecode = softwaresourcecode;
    }

    public String getEntityInstanceId() {
        return id != null ? id.getEntityInstanceId() : null;
    }

    public void setEntityInstanceId(String entityInstanceId) {
        if (id == null) id = new SoftwaresourcecodeCreatorId();
        id.setEntityInstanceId(entityInstanceId);
    }

    public String getResourceEntity() {
        return id != null ? id.getResourceEntity() : null;
    }

    public void setResourceEntity(String resourceEntity) {
        if (id == null) id = new SoftwaresourcecodeCreatorId();
        id.setResourceEntity(resourceEntity);
    }
}
