package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "softwareapplication_operation", schema = "metadata_catalogue")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class SoftwareapplicationOperation {
    @EmbeddedId
    private SoftwareapplicationOperationId id;

    @MapsId("softwareapplicationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "softwareapplication_instance_id", nullable = false)
    private Softwareapplication softwareapplicationInstance;

    @MapsId("operationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_instance_id", nullable = false)
    private Operation operationInstance;

    public SoftwareapplicationOperationId getId() {
        return id;
    }

    public void setId(SoftwareapplicationOperationId id) {
        this.id = id;
    }

    public Softwareapplication getSoftwareapplicationInstance() {
        return softwareapplicationInstance;
    }

    public void setSoftwareapplicationInstance(Softwareapplication softwareapplicationInstance) {
        this.softwareapplicationInstance = softwareapplicationInstance;
    }

    public Operation getOperationInstance() {
        return operationInstance;
    }

    public void setOperationInstance(Operation operationInstance) {
        this.operationInstance = operationInstance;
    }

}