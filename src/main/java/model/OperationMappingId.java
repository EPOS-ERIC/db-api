package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class OperationMappingId implements java.io.Serializable {
    private static final long serialVersionUID = 7881063328122642937L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "operation_instance_id", nullable = false, length = 100)
    private String operationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "mapping_instance_id", nullable = false, length = 100)
    private String mappingInstanceId;

    public String getOperationInstanceId() {
        return operationInstanceId;
    }

    public void setOperationInstanceId(String operationInstanceId) {
        this.operationInstanceId = operationInstanceId;
    }

    public String getMappingInstanceId() {
        return mappingInstanceId;
    }

    public void setMappingInstanceId(String mappingInstanceId) {
        this.mappingInstanceId = mappingInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        OperationMappingId entity = (OperationMappingId) o;
        return Objects.equals(this.mappingInstanceId, entity.mappingInstanceId) &&
                Objects.equals(this.operationInstanceId, entity.operationInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappingInstanceId, operationInstanceId);
    }

}