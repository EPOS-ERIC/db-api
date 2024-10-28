package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class DataproductCategoryId implements java.io.Serializable {
    private static final long serialVersionUID = 6942342642381589107L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "dataproduct_instance_id", nullable = false, length = 100)
    private String dataproductInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category_instance_id", nullable = false, length = 100)
    private String categoryInstanceId;

    public String getDataproductInstanceId() {
        return dataproductInstanceId;
    }

    public void setDataproductInstanceId(String dataproductInstanceId) {
        this.dataproductInstanceId = dataproductInstanceId;
    }

    public String getCategoryInstanceId() {
        return categoryInstanceId;
    }

    public void setCategoryInstanceId(String categoryInstanceId) {
        this.categoryInstanceId = categoryInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        DataproductCategoryId entity = (DataproductCategoryId) o;
        return Objects.equals(this.dataproductInstanceId, entity.dataproductInstanceId) &&
                Objects.equals(this.categoryInstanceId, entity.categoryInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataproductInstanceId, categoryInstanceId);
    }

}