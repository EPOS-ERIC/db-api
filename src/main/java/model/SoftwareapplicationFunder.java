package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "softwareapplication_funder", schema = "metadata_catalogue")
public class SoftwareapplicationFunder {
    @Id
    @Size(max = 100)
    @Column(name = "softwareapplication_instance_id", nullable = false, length = 100)
    private String softwareapplicationInstanceId;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "softwareapplication_instance_id", nullable = false)
    private Softwareapplication softwareapplication;

    @Size(max = 100)
    @NotNull
    @Column(name = "entity_instance_id", nullable = false, length = 100)
    private String entityInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "resource_entity", nullable = false, length = 100)
    private String resourceEntity;

    public String getSoftwareapplicationInstanceId() {
        return softwareapplicationInstanceId;
    }

    public void setSoftwareapplicationInstanceId(String softwareapplicationInstanceId) {
        this.softwareapplicationInstanceId = softwareapplicationInstanceId;
    }

    public Softwareapplication getSoftwareapplication() {
        return softwareapplication;
    }

    public void setSoftwareapplication(Softwareapplication softwareapplication) {
        this.softwareapplication = softwareapplication;
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