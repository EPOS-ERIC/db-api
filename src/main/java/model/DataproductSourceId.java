package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.util.ProxyUtils;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DataproductSourceId implements Serializable {
    private static final long serialVersionUID = -3287168331205868120L;
    @Size(max = 100)
    @NotNull
    @Column(name = "dataproduct1_instance_id", nullable = false, length = 100)
    private String dataproduct1InstanceId;

    @Size(max = 100)
    @NotNull
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
        if (o == null || ProxyUtils.getUserClass(this) != ProxyUtils.getUserClass(o)) return false;
        DataproductSourceId entity = (DataproductSourceId) o;
        return Objects.equals(this.dataproduct2InstanceId, entity.dataproduct2InstanceId) &&
                Objects.equals(this.dataproduct1InstanceId, entity.dataproduct1InstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataproduct2InstanceId, dataproduct1InstanceId);
    }

}