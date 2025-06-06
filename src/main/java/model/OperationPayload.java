package model;

import jakarta.persistence.*;

@Entity
@Table(name = "operation_payload", schema = "metadata_catalogue")
public class OperationPayload {
    @EmbeddedId
    private OperationPayloadId id;

    @MapsId("operationInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "operation_instance_id", nullable = false)
    private Operation operationInstance;

    @MapsId("payloadInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "payload_instance_id", nullable = false)
    private model.Payload payloadInstance;

    public OperationPayloadId getId() {
        return id;
    }

    public void setId(OperationPayloadId id) {
        this.id = id;
    }

    public Operation getOperationInstance() {
        return operationInstance;
    }

    public void setOperationInstance(Operation operationInstance) {
        this.operationInstance = operationInstance;
    }

    public model.Payload getPayloadInstance() {
        return payloadInstance;
    }

    public void setPayloadInstance(model.Payload payloadInstance) {
        this.payloadInstance = payloadInstance;
    }

}