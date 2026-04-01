package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwareapplication_creator", schema = "metadata_catalogue")
public class SoftwareapplicationCreator {
    @EmbeddedId
    private SoftwareapplicationCreatorId id;

    @MapsId("softwareapplicationInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "softwareapplication_instance_id", nullable = false)
    private Softwareapplication softwareapplication;

    public SoftwareapplicationCreator() {
        this.id = new SoftwareapplicationCreatorId();
    }

    public SoftwareapplicationCreatorId getId() {
        return id;
    }

    public void setId(SoftwareapplicationCreatorId id) {
        this.id = id;
    }

    public String getSoftwareapplicationInstanceId() {
        return id != null ? id.getSoftwareapplicationInstanceId() : null;
    }

    public void setSoftwareapplicationInstanceId(String softwareapplicationInstanceId) {
        if (id == null) id = new SoftwareapplicationCreatorId();
        id.setSoftwareapplicationInstanceId(softwareapplicationInstanceId);
    }

    public Softwareapplication getSoftwareapplication() {
        return softwareapplication;
    }

    public void setSoftwareapplication(Softwareapplication softwareapplication) {
        this.softwareapplication = softwareapplication;
    }

    public String getEntityInstanceId() {
        return id != null ? id.getEntityInstanceId() : null;
    }

    public void setEntityInstanceId(String entityInstanceId) {
        if (id == null) id = new SoftwareapplicationCreatorId();
        id.setEntityInstanceId(entityInstanceId);
    }

    public String getResourceEntity() {
        return id != null ? id.getResourceEntity() : null;
    }

    public void setResourceEntity(String resourceEntity) {
        if (id == null) id = new SoftwareapplicationCreatorId();
        id.setResourceEntity(resourceEntity);
    }
}
