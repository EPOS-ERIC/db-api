package model;

import jakarta.persistence.*;

@Entity
@Table(name = "operation_distribution", schema = "metadata_catalogue")
public class OperationDistribution {
    @EmbeddedId
    private OperationDistributionId id;

    @MapsId("distributionInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_instance_id", nullable = false)
    private Distribution distributionInstance;

    @MapsId("operationInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_instance_id", nullable = false)
    private Operation operationInstance;

    public OperationDistributionId getId() {
        return id;
    }

    public void setId(OperationDistributionId id) {
        this.id = id;
    }

    public Distribution getDistributionInstance() {
        return distributionInstance;
    }

    public void setDistributionInstance(Distribution distributionInstance) {
        this.distributionInstance = distributionInstance;
    }

    public Operation getOperationInstance() {
        return operationInstance;
    }

    public void setOperationInstance(Operation operationInstance) {
        this.operationInstance = operationInstance;
    }

}