package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class CategoryIspartofId implements java.io.Serializable {
    private static final long serialVersionUID = -6277606164201800457L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category1_instance_id", nullable = false, length = 100)
    private String category1InstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category2_instance_id", nullable = false, length = 100)
    private String category2InstanceId;

    public String getCategory1InstanceId() {
        return category1InstanceId;
    }

    public void setCategory1InstanceId(String category1InstanceId) {
        this.category1InstanceId = category1InstanceId;
    }

    public String getCategory2InstanceId() {
        return category2InstanceId;
    }

    public void setCategory2InstanceId(String category2InstanceId) {
        this.category2InstanceId = category2InstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        CategoryIspartofId entity = (CategoryIspartofId) o;
        return Objects.equals(this.category2InstanceId, entity.category2InstanceId) &&
                Objects.equals(this.category1InstanceId, entity.category1InstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category2InstanceId, category1InstanceId);
    }

}