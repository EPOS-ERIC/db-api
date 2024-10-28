package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class PersonIdentifierId implements java.io.Serializable {
    private static final long serialVersionUID = 2488275307917460951L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "person_instance_id", nullable = false, length = 100)
    private String personInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "identifier_instance_id", nullable = false, length = 100)
    private String identifierInstanceId;

    public String getPersonInstanceId() {
        return personInstanceId;
    }

    public void setPersonInstanceId(String personInstanceId) {
        this.personInstanceId = personInstanceId;
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
        PersonIdentifierId entity = (PersonIdentifierId) o;
        return Objects.equals(this.personInstanceId, entity.personInstanceId) &&
                Objects.equals(this.identifierInstanceId, entity.identifierInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personInstanceId, identifierInstanceId);
    }

}