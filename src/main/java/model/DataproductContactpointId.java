package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class DataproductContactpointId implements java.io.Serializable {
    private static final long serialVersionUID = -8442013337361472815L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "dataproduct_instance_id", nullable = false, length = 100)
    private String dataproductInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "contactpoint_instance_id", nullable = false, length = 100)
    private String contactpointInstanceId;

    public String getDataproductInstanceId() {
        return dataproductInstanceId;
    }

    public void setDataproductInstanceId(String dataproductInstanceId) {
        this.dataproductInstanceId = dataproductInstanceId;
    }

    public String getContactpointInstanceId() {
        return contactpointInstanceId;
    }

    public void setContactpointInstanceId(String contactpointInstanceId) {
        this.contactpointInstanceId = contactpointInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        DataproductContactpointId entity = (DataproductContactpointId) o;
        return Objects.equals(this.dataproductInstanceId, entity.dataproductInstanceId) &&
                Objects.equals(this.contactpointInstanceId, entity.contactpointInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataproductInstanceId, contactpointInstanceId);
    }

}