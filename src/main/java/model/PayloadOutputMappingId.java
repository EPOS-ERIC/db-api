package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.util.ProxyUtils;

import java.util.Objects;

@Embeddable
public class PayloadOutputMappingId implements java.io.Serializable {
    private static final long serialVersionUID = 3729712795386222645L;
    @Size(max = 100)
    @NotNull
    @Column(name = "payload_instance_id", nullable = false, length = 100)
    private String payloadInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "output_mapping_instance_id", nullable = false, length = 100)
    private String outputMappingInstanceId;

    public String getPayloadInstanceId() {
        return payloadInstanceId;
    }

    public void setPayloadInstanceId(String payloadInstanceId) {
        this.payloadInstanceId = payloadInstanceId;
    }

    public String getOutputMappingInstanceId() {
        return outputMappingInstanceId;
    }

    public void setOutputMappingInstanceId(String outputMappingInstanceId) {
        this.outputMappingInstanceId = outputMappingInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ProxyUtils.getUserClass(this) != ProxyUtils.getUserClass(o)) return false;
        PayloadOutputMappingId entity = (PayloadOutputMappingId) o;
        return Objects.equals(this.outputMappingInstanceId, entity.outputMappingInstanceId) &&
                Objects.equals(this.payloadInstanceId, entity.payloadInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outputMappingInstanceId, payloadInstanceId);
    }

}