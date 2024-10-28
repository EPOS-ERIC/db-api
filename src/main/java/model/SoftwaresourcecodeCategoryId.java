package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class SoftwaresourcecodeCategoryId implements java.io.Serializable {
    private static final long serialVersionUID = -7242139103532185569L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "softwaresourcecode_instance_id", nullable = false, length = 100)
    private String softwaresourcecodeInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category_instance_id", nullable = false, length = 100)
    private String categoryInstanceId;

    public String getSoftwaresourcecodeInstanceId() {
        return softwaresourcecodeInstanceId;
    }

    public void setSoftwaresourcecodeInstanceId(String softwaresourcecodeInstanceId) {
        this.softwaresourcecodeInstanceId = softwaresourcecodeInstanceId;
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
        SoftwaresourcecodeCategoryId entity = (SoftwaresourcecodeCategoryId) o;
        return Objects.equals(this.softwaresourcecodeInstanceId, entity.softwaresourcecodeInstanceId) &&
                Objects.equals(this.categoryInstanceId, entity.categoryInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwaresourcecodeInstanceId, categoryInstanceId);
    }

}