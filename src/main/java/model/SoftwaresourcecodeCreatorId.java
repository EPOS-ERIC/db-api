package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SoftwaresourcecodeCreatorId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Size(max = 100)
    @NotNull
    @Column(name = "softwaresourcecode_instance_id", nullable = false, length = 100)
    private String softwaresourcecodeInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "entity_instance_id", nullable = false, length = 100)
    private String entityInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "resource_entity", nullable = false, length = 100)
    private String resourceEntity;

    public SoftwaresourcecodeCreatorId() {}

    public SoftwaresourcecodeCreatorId(String softwaresourcecodeInstanceId, String entityInstanceId, String resourceEntity) {
        this.softwaresourcecodeInstanceId = softwaresourcecodeInstanceId;
        this.entityInstanceId = entityInstanceId;
        this.resourceEntity = resourceEntity;
    }

    public String getSoftwaresourcecodeInstanceId() {
        return softwaresourcecodeInstanceId;
    }

    public void setSoftwaresourcecodeInstanceId(String softwaresourcecodeInstanceId) {
        this.softwaresourcecodeInstanceId = softwaresourcecodeInstanceId;
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
        SoftwaresourcecodeCreatorId that = (SoftwaresourcecodeCreatorId) o;
        return Objects.equals(softwaresourcecodeInstanceId, that.softwaresourcecodeInstanceId) &&
                Objects.equals(entityInstanceId, that.entityInstanceId) &&
                Objects.equals(resourceEntity, that.resourceEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwaresourcecodeInstanceId, entityInstanceId, resourceEntity);
    }
}
