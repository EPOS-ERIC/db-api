package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class PublicationIdentifierId implements java.io.Serializable {
    private static final long serialVersionUID = 7998697085149943834L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "publication_instance_id", nullable = false, length = 100)
    private String publicationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "identifier_instance_id", nullable = false, length = 100)
    private String identifierInstanceId;

    public String getPublicationInstanceId() {
        return publicationInstanceId;
    }

    public void setPublicationInstanceId(String publicationInstanceId) {
        this.publicationInstanceId = publicationInstanceId;
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
        PublicationIdentifierId entity = (PublicationIdentifierId) o;
        return Objects.equals(this.publicationInstanceId, entity.publicationInstanceId) &&
                Objects.equals(this.identifierInstanceId, entity.identifierInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicationInstanceId, identifierInstanceId);
    }

}