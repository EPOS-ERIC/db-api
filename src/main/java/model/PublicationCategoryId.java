package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class PublicationCategoryId implements java.io.Serializable {
    private static final long serialVersionUID = -6300677975775755366L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "publication_instance_id", nullable = false, length = 100)
    private String publicationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category_instance_id", nullable = false, length = 100)
    private String categoryInstanceId;

    public String getPublicationInstanceId() {
        return publicationInstanceId;
    }

    public void setPublicationInstanceId(String publicationInstanceId) {
        this.publicationInstanceId = publicationInstanceId;
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
        PublicationCategoryId entity = (PublicationCategoryId) o;
        return Objects.equals(this.publicationInstanceId, entity.publicationInstanceId) &&
                Objects.equals(this.categoryInstanceId, entity.categoryInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicationInstanceId, categoryInstanceId);
    }

}