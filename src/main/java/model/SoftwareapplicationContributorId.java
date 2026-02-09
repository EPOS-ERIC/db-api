package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SoftwareapplicationContributorId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Size(max = 100)
    @NotNull
    @Column(name = "softwareapplication_instance_id", nullable = false, length = 100)
    private String softwareapplicationInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "entity_instance_id", nullable = false, length = 100)
    private String entityInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "resource_entity", nullable = false, length = 100)
    private String resourceEntity;

    public SoftwareapplicationContributorId() {}

    public SoftwareapplicationContributorId(String softwareapplicationInstanceId, String entityInstanceId, String resourceEntity) {
        this.softwareapplicationInstanceId = softwareapplicationInstanceId;
        this.entityInstanceId = entityInstanceId;
        this.resourceEntity = resourceEntity;
    }

    public String getSoftwareapplicationInstanceId() {
        return softwareapplicationInstanceId;
    }

    public void setSoftwareapplicationInstanceId(String softwareapplicationInstanceId) {
        this.softwareapplicationInstanceId = softwareapplicationInstanceId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SoftwareapplicationContributorId that = (SoftwareapplicationContributorId) o;
        return Objects.equals(softwareapplicationInstanceId, that.softwareapplicationInstanceId) &&
                Objects.equals(entityInstanceId, that.entityInstanceId) &&
                Objects.equals(resourceEntity, that.resourceEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwareapplicationInstanceId, entityInstanceId, resourceEntity);
    }
}
