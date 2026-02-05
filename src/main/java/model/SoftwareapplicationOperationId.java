package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class SoftwareapplicationOperationId implements java.io.Serializable {
    private static final long serialVersionUID = 4290202745506088695L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "softwareapplication_instance_id", nullable = false, length = 100)
    private String softwareapplicationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "operation_instance_id", nullable = false, length = 100)
    private String operationInstanceId;

    public String getSoftwareapplicationInstanceId() {
        return softwareapplicationInstanceId;
    }

    public void setSoftwareapplicationInstanceId(String softwareapplicationInstanceId) {
        this.softwareapplicationInstanceId = softwareapplicationInstanceId;
    }

    public String getOperationInstanceId() {
        return operationInstanceId;
    }

    public void setOperationInstanceId(String operationInstanceId) {
        this.operationInstanceId = operationInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        SoftwareapplicationOperationId entity = (SoftwareapplicationOperationId) o;
        return Objects.equals(this.softwareapplicationInstanceId, entity.softwareapplicationInstanceId) &&
                Objects.equals(this.operationInstanceId, entity.operationInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwareapplicationInstanceId, operationInstanceId);
    }

}