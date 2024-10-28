package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class DataproductIdentifierId implements java.io.Serializable {
    private static final long serialVersionUID = -7971062091550892930L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "dataproduct_instance_id", nullable = false, length = 100)
    private String dataproductInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "identifier_instance_id", nullable = false, length = 100)
    private String identifierInstanceId;

    public String getDataproductInstanceId() {
        return dataproductInstanceId;
    }

    public void setDataproductInstanceId(String dataproductInstanceId) {
        this.dataproductInstanceId = dataproductInstanceId;
    }

    public String getIdentifierInstanceId() {
        return identifierInstanceId;
    }

    public void setIdentifierInstanceId(String identifierInstanceId) {
        this.identifierInstanceId = identifierInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        DataproductIdentifierId entity = (DataproductIdentifierId) o;
        return Objects.equals(this.dataproductInstanceId, entity.dataproductInstanceId) &&
                Objects.equals(this.identifierInstanceId, entity.identifierInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataproductInstanceId, identifierInstanceId);
    }

}