package model;

import jakarta.persistence.*;

@Entity
@Table(name = "softwareapplication_author", schema = "metadata_catalogue")
public class SoftwareapplicationAuthor {
    @EmbeddedId
    private SoftwareapplicationAuthorId id;

    @MapsId("softwareapplicationInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "softwareapplication_instance_id", nullable = false)
    private Softwareapplication softwareapplication;

    public SoftwareapplicationAuthor() {
        this.id = new SoftwareapplicationAuthorId();
    }

    public SoftwareapplicationAuthorId getId() {
        return id;
    }

    public void setId(SoftwareapplicationAuthorId id) {
        this.id = id;
    }

    public String getSoftwareapplicationInstanceId() {
        return id != null ? id.getSoftwareapplicationInstanceId() : null;
    }

    public void setSoftwareapplicationInstanceId(String softwareapplicationInstanceId) {
        if (id == null) id = new SoftwareapplicationAuthorId();
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
        if (id == null) id = new SoftwareapplicationAuthorId();
        id.setEntityInstanceId(entityInstanceId);
    }

    public String getResourceEntity() {
        return id != null ? id.getResourceEntity() : null;
    }

    public void setResourceEntity(String resourceEntity) {
        if (id == null) id = new SoftwareapplicationAuthorId();
        id.setResourceEntity(resourceEntity);
    }
}
