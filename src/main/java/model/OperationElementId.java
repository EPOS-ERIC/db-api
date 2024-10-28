package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;

import java.util.Objects;

@Embeddable
public class OperationElementId implements java.io.Serializable {
    private static final long serialVersionUID = 5642978579805556911L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "operation_instance_id", nullable = false, length = 100)
    private String operationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "element_instance_id", nullable = false, length = 100)
    private String elementInstanceId;

    public String getOperationInstanceId() {
        return operationInstanceId;
    }

    public void setOperationInstanceId(String operationInstanceId) {
        this.operationInstanceId = operationInstanceId;
    }

    public String getElementInstanceId() {
        return elementInstanceId;
    }

    public void setElementInstanceId(String elementInstanceId) {
        this.elementInstanceId = elementInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        OperationElementId entity = (OperationElementId) o;
        return Objects.equals(this.elementInstanceId, entity.elementInstanceId) &&
                Objects.equals(this.operationInstanceId, entity.operationInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementInstanceId, operationInstanceId);
    }

}