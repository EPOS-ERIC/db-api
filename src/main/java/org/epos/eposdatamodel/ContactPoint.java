package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A Contact Point of a Class. it can be refered to phisical person or an organization
 */
public class ContactPoint extends EPOSDataModelEntity {

    /**
     * This property refers to the Contact Point Email.
     **/
    @Schema(name = "email", description = "This property refers to the Contact Point Email.", example = "[\"email@email.com\"]", required = false)
    private List<String> email = null;

    /**
     * This property contains information about the language used by the Contact
     * Point. Please use one of the language codes from the IETF BCP 47 standard.
     */
    @Schema(name = "language", description = "This property contains information about the language used by the Contact Point using the language codes from the IETF BCP 47 standard.", example = "[\"en\"]", required = false)
    private List<String> language = null;

    /**
     * This property refers to the Contact Point role.
     **/
    @Schema(name = "role", description = "This property refers to the Contact Point role.", example = "manager", required = false)
    private String role = null;

    /**
     * This property refers to the related Organization which represents the contactpoint.
     */
    @Schema(name = "organization", description = "This property refers to the related Organization which represents the contactpoint.", example = "{\n" +
            "    \"entityType\": \"ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }", required = false)
    private LinkedEntity organization = null;

    /**
     * This property refers to the related Person which represents the contact point.
     */
    @Schema(name = "person", description = "This property refers to the related Person which represents the contact point.", example = "{\n" +
            "    \"entityType\": \"PERSON\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }", required = false)
    private LinkedEntity person = null;

    /**
     * This property refers to the Contact Point Telephone.
     **/
    @Schema(name = "telephone", description = "This property refers to the Contact Point Telephone.", example = "[\"+0039213123164\"]", required = false)
    private List<String> telephone = null;

    public void addEmail(String email) {
        if (this.email == null) {
            ArrayList<String> emailList = new ArrayList<>();
            emailList.add(email);
            this.setEmail(emailList);
        } else {
            this.email.add(email);
        }
    }

    public void addTelephone(String telephone) {
        if (this.telephone == null) {
            ArrayList<String> telephoneList = new ArrayList<>();
            telephoneList.add(telephone);
            this.setTelephone(telephoneList);
        } else {
            this.telephone.add(telephone);
        }
    }

    public void addLanguage(String language) {
        if (this.language == null) {
            ArrayList<String> languageList = new ArrayList<>();
            languageList.add(language);
            this.setLanguage(languageList);
        } else {
            this.language.add(language);
        }
    }


    public ContactPoint email(List<String> email) {
        this.email = email;
        return this;
    }

    public ContactPoint addEmailItem(String emailItem) {
        if (this.email == null) {
            this.email = new ArrayList<>();
        }
        this.email.add(emailItem);
        return this;
    }

    /**
     * This property refers to the Contact Point Email.
     *
     * @return email
     **/

    public List<String> getEmail() {
        return email;
    }

    public void setEmail(List<String> email) {
        this.email = email;
    }

    public List<String> getLanguage() {
        return language;
    }

    public void setLanguage(List<String> language) {
        this.language = language;
    }

    public ContactPoint role(String role) {
        this.role = role;
        return this;
    }

    /**
     * This property refers to the Contact Point role.
     *
     * @return role
     **/

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public ContactPoint telephone(List<String> telephone) {
        this.telephone = telephone;
        return this;
    }

    public ContactPoint addTelephoneItem(String telephoneItem) {
        if (this.telephone == null) {
            this.telephone = new ArrayList<>();
        }
        this.telephone.add(telephoneItem);
        return this;
    }

    /**
     * This property refers to the Contact Point Telephone.
     *
     * @return telephone
     **/

    public List<String> getTelephone() {
        return telephone;
    }

    public void setTelephone(List<String> telephone) {
        this.telephone = telephone;
    }


    public LinkedEntity getOrganization() {
        return organization;
    }

    public void setOrganization(LinkedEntity organization) {
        this.organization = organization;
    }

    public LinkedEntity getPerson() {
        return person;
    }

    public void setPerson(LinkedEntity person) {
        this.person = person;
    }

    @Override
    public String toString() {
        return "ContactPoint{" +
                "email=" + email +
                ", language=" + language +
                ", role='" + role + '\'' +
                ", organization=" + organization +
                ", person=" + person +
                ", telephone=" + telephone +
                '}'+ super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ContactPoint that = (ContactPoint) o;
        return Objects.equals(getEmail(), that.getEmail()) && Objects.equals(getLanguage(), that.getLanguage()) && Objects.equals(getRole(), that.getRole()) && Objects.equals(getOrganization(), that.getOrganization()) && Objects.equals(getPerson(), that.getPerson()) && Objects.equals(getTelephone(), that.getTelephone());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getEmail(), getLanguage(), getRole(), getOrganization(), getPerson(), getTelephone());
    }
}
