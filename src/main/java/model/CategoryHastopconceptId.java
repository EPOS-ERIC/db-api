package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class CategoryHastopconceptId implements java.io.Serializable {
    private static final long serialVersionUID = 2477521682557719511L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category_scheme_instance_id", nullable = false, length = 100)
    private String categorySchemeInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category_instance_id", nullable = false, length = 100)
    private String categoryInstanceId;

    public String getCategorySchemeInstanceId() {
        return categorySchemeInstanceId;
    }

    public void setCategorySchemeInstanceId(String categorySchemeInstanceId) {
        this.categorySchemeInstanceId = categorySchemeInstanceId;
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
        CategoryHastopconceptId entity = (CategoryHastopconceptId) o;
        return Objects.equals(this.categoryInstanceId, entity.categoryInstanceId) &&
                Objects.equals(this.categorySchemeInstanceId, entity.categorySchemeInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryInstanceId, categorySchemeInstanceId);
    }

}