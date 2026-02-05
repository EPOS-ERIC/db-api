package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.util.ProxyUtils;

import java.util.Objects;

@Embeddable
public class OperationPayloadId implements java.io.Serializable {
    private static final long serialVersionUID = 2277289962495562779L;
    @Size(max = 100)
    @NotNull
    @Column(name = "operation_instance_id", nullable = false, length = 100)
    private String operationInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "payload_instance_id", nullable = false, length = 100)
    private String payloadInstanceId;

    public String getOperationInstanceId() {
        return operationInstanceId;
    }

    public void setOperationInstanceId(String operationInstanceId) {
        this.operationInstanceId = operationInstanceId;
    }

    public String getPayloadInstanceId() {
        return payloadInstanceId;
    }

    public void setPayloadInstanceId(String payloadInstanceId) {
        this.payloadInstanceId = payloadInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ProxyUtils.getUserClass(this) != ProxyUtils.getUserClass(o)) return false;
        OperationPayloadId entity = (OperationPayloadId) o;
        return Objects.equals(this.operationInstanceId, entity.operationInstanceId) &&
                Objects.equals(this.payloadInstanceId, entity.payloadInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationInstanceId, payloadInstanceId);
    }

}