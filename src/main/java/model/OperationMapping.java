package model;

import jakarta.persistence.*;

@Entity
@Table(name = "operation_mapping")
public class OperationMapping {
    @EmbeddedId
    private OperationMappingId id;

    @MapsId("operationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "operation_instance_id", nullable = false)
    private Operation operationInstance;

    @MapsId("mappingInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "mapping_instance_id", nullable = false)
    private Mapping mappingInstance;

    public OperationMappingId getId() {
        return id;
    }

    public void setId(OperationMappingId id) {
        this.id = id;
    }

    public Operation getOperationInstance() {
        return operationInstance;
    }

    public void setOperationInstance(Operation operationInstance) {
        this.operationInstance = operationInstance;
    }

    public Mapping getMappingInstance() {
        return mappingInstance;
    }

    public void setMappingInstance(Mapping mappingInstance) {
        this.mappingInstance = mappingInstance;
    }

}