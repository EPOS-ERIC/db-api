package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class DataproductHaspartId implements java.io.Serializable {
    private static final long serialVersionUID = -2265666889293702801L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "dataproduct1_instance_id", nullable = false, length = 100)
    private String dataproduct1InstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "dataproduct2_instance_id", nullable = false, length = 100)
    private String dataproduct2InstanceId;

    public String getDataproduct1InstanceId() {
        return dataproduct1InstanceId;
    }

    public void setDataproduct1InstanceId(String dataproduct1InstanceId) {
        this.dataproduct1InstanceId = dataproduct1InstanceId;
    }

    public String getDataproduct2InstanceId() {
        return dataproduct2InstanceId;
    }

    public void setDataproduct2InstanceId(String dataproduct2InstanceId) {
        this.dataproduct2InstanceId = dataproduct2InstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        DataproductHaspartId entity = (DataproductHaspartId) o;
        return Objects.equals(this.dataproduct2InstanceId, entity.dataproduct2InstanceId) &&
                Objects.equals(this.dataproduct1InstanceId, entity.dataproduct1InstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataproduct2InstanceId, dataproduct1InstanceId);
    }

}