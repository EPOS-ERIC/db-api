package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class DataproductSpatialId implements java.io.Serializable {
    private static final long serialVersionUID = 329165227144261860L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "dataproduct_instance_id", nullable = false, length = 100)
    private String dataproductInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "spatial_instance_id", nullable = false, length = 100)
    private String spatialInstanceId;

    public String getDataproductInstanceId() {
        return dataproductInstanceId;
    }

    public void setDataproductInstanceId(String dataproductInstanceId) {
        this.dataproductInstanceId = dataproductInstanceId;
    }

    public String getSpatialInstanceId() {
        return spatialInstanceId;
    }

    public void setSpatialInstanceId(String spatialInstanceId) {
        this.spatialInstanceId = spatialInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        DataproductSpatialId entity = (DataproductSpatialId) o;
        return Objects.equals(this.dataproductInstanceId, entity.dataproductInstanceId) &&
                Objects.equals(this.spatialInstanceId, entity.spatialInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataproductInstanceId, spatialInstanceId);
    }

}