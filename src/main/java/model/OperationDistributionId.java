package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class OperationDistributionId implements java.io.Serializable {
    private static final long serialVersionUID = -3473958724091069310L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "distribution_instance_id", nullable = false, length = 100)
    private String distributionInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "operation_instance_id", nullable = false, length = 100)
    private String operationInstanceId;

    public String getDistributionInstanceId() {
        return distributionInstanceId;
    }

    public void setDistributionInstanceId(String distributionInstanceId) {
        this.distributionInstanceId = distributionInstanceId;
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
        OperationDistributionId entity = (OperationDistributionId) o;
        return Objects.equals(this.distributionInstanceId, entity.distributionInstanceId) &&
                Objects.equals(this.operationInstanceId, entity.operationInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distributionInstanceId, operationInstanceId);
    }

}