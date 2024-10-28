package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class PublicationContributorId implements java.io.Serializable {
    private static final long serialVersionUID = -2447566844994211271L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "publication_instance_id", nullable = false, length = 100)
    private String publicationInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "person_instance_id", nullable = false, length = 100)
    private String personInstanceId;

    public String getPublicationInstanceId() {
        return publicationInstanceId;
    }

    public void setPublicationInstanceId(String publicationInstanceId) {
        this.publicationInstanceId = publicationInstanceId;
    }

    public String getPersonInstanceId() {
        return personInstanceId;
    }

    public void setPersonInstanceId(String personInstanceId) {
        this.personInstanceId = personInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        PublicationContributorId entity = (PublicationContributorId) o;
        return Objects.equals(this.publicationInstanceId, entity.publicationInstanceId) &&
                Objects.equals(this.personInstanceId, entity.personInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicationInstanceId, personInstanceId);
    }

}