package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "softwaresourcecode_publisher", schema = "metadata_catalogue")
public class SoftwaresourcecodePublisher {
    @Id
    @Size(max = 100)
    @Column(name = "softwaresourcecode_instance_id", nullable = false, length = 100)
    private String softwaresourcecodeInstanceId;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "softwaresourcecode_instance_id", nullable = false)
    private Softwaresourcecode softwaresourcecode;

    @Size(max = 100)
    @NotNull
    @Column(name = "entity_instance_id", nullable = false, length = 100)
    private String entityInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "resource_entity", nullable = false, length = 100)
    private String resourceEntity;

    public String getSoftwaresourcecodeInstanceId() {
        return softwaresourcecodeInstanceId;
    }

    public void setSoftwaresourcecodeInstanceId(String softwaresourcecodeInstanceId) {
        this.softwaresourcecodeInstanceId = softwaresourcecodeInstanceId;
    }

    public Softwaresourcecode getSoftwaresourcecode() {
        return softwaresourcecode;
    }

    public void setSoftwaresourcecode(Softwaresourcecode softwaresourcecode) {
        this.softwaresourcecode = softwaresourcecode;
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