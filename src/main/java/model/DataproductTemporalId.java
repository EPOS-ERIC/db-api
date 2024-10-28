package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class DataproductTemporalId implements java.io.Serializable {
    private static final long serialVersionUID = 4553413059160324153L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "dataproduct_instance_id", nullable = false, length = 100)
    private String dataproductInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "temporal_instance_id", nullable = false, length = 100)
    private String temporalInstanceId;

    public String getDataproductInstanceId() {
        return dataproductInstanceId;
    }

    public void setDataproductInstanceId(String dataproductInstanceId) {
        this.dataproductInstanceId = dataproductInstanceId;
    }

    public String getTemporalInstanceId() {
        return temporalInstanceId;
    }

    public void setTemporalInstanceId(String temporalInstanceId) {
        this.temporalInstanceId = temporalInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        DataproductTemporalId entity = (DataproductTemporalId) o;
        return Objects.equals(this.dataproductInstanceId, entity.dataproductInstanceId) &&
                Objects.equals(this.temporalInstanceId, entity.temporalInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataproductInstanceId, temporalInstanceId);
    }

}