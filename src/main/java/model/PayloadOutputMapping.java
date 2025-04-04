package model;

import jakarta.persistence.*;

@Entity
@Table(name = "payload_output_mapping")
public class PayloadOutputMapping {
    @EmbeddedId
    private PayloadOutputMappingId id;

    @MapsId("payloadInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payload_instance_id", nullable = false)
    private Payload payloadInstance;

    @MapsId("outputMappingInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_mapping_instance_id", nullable = false)
    private OutputMapping outputMappingInstance;

    public PayloadOutputMappingId getId() {
        return id;
    }

    public void setId(PayloadOutputMappingId id) {
        this.id = id;
    }

    public Payload getPayloadInstance() {
        return payloadInstance;
    }

    public void setPayloadInstance(Payload payloadInstance) {
        this.payloadInstance = payloadInstance;
    }

    public OutputMapping getOutputMappingInstance() {
        return outputMappingInstance;
    }

    public void setOutputMappingInstance(OutputMapping outputMappingInstance) {
        this.outputMappingInstance = outputMappingInstance;
    }

}