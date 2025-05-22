package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.util.ProxyUtils;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DataproductAttributionId implements Serializable {
    private static final long serialVersionUID = 6578990412386514909L;
    @Size(max = 100)
    @NotNull
    @Column(name = "dataproduct_instance_id", nullable = false, length = 100)
    private String dataproductInstanceId;

    @Size(max = 100)
    @NotNull
    @Column(name = "attribution_instance_id", nullable = false, length = 100)
    private String attributionInstanceId;

    public String getDataproductInstanceId() {
        return dataproductInstanceId;
    }

    public void setDataproductInstanceId(String dataproductInstanceId) {
        this.dataproductInstanceId = dataproductInstanceId;
    }

    public String getAttributionInstanceId() {
        return attributionInstanceId;
    }

    public void setAttributionInstanceId(String attributionInstanceId) {
        this.attributionInstanceId = attributionInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ProxyUtils.getUserClass(this) != ProxyUtils.getUserClass(o)) return false;
        DataproductAttributionId entity = (DataproductAttributionId) o;
        return Objects.equals(this.dataproductInstanceId, entity.dataproductInstanceId) &&
                Objects.equals(this.attributionInstanceId, entity.attributionInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataproductInstanceId, attributionInstanceId);
    }

}