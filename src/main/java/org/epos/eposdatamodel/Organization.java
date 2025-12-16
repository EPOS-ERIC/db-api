package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Institution or International organization.
 */
public class Organization extends EPOSDataModelEntity {

    /**
     * This property contains the physical address of the Organization.
     **/
    @Schema(name = "address", description = "This property contains the physical address of the Organization.", example = "{\n" +
            "    \"entityType\": \"ADDRESS\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }", required = false)
    private LinkedEntity address;

    /**
     * This property refers to the Contact Point (i.e. Role) defined for the Organization
     */
    @Schema(name = "contactPoint", description = "This property refers to the Contact Point (i.e. Role) defined for the Organization", example = "[{\n" +
            "    \"entityType\": \"CONTACTPOINT\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> contactPoint;

    /**
     * This property contains the email address of the Organization.
     **/
    @Schema(name = "email", description = "This property contains the email address of the Organization.", example = "[\"email@email.com\"]", required = false)
    private List<String> email;

    /**
     * This property contains an identifier for the Organization (e.g., PIC, ISNI, etc.). LinkedEntity of type IDENTIFIER
     **/
    @Schema(name = "identifier", description = "This property contains an identifier for the Organization (e.g., PIC, ISNI, etc.).", example = "[{\n" +
            "    \"entityType\": \"IDENTIFIER\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> identifier = new ArrayList<>();

    /**
     * This property contains the Organization acronym (e.g., INGV).
     */
    @Schema(name = "acronym", description = "This property contains the Organization acronym (e.g., INGV).", example = "INGV", required = false)
    private String acronym;

    /**
     * This property contains the official name of the Organization. LinkedEntity of type LEGALNAME
     **/
    @Schema(name = "legalName", description = "This property contains the Organization legalname (e.g., Istituto Nazionale di Geofisica e Vulcanologia).", example = " Istituto Nazionale di Geofisica e Vulcanologia", required = false)
    private List<String> legalName;

    /**
     * This property contains the Organization identifier that uniquely identifies a legal entity as defined in ISO 17442.
     **/
    @Schema(name = "leiCode", description = "This property contains the Organization identifier that uniquely identifies a legal entity as defined in ISO 17442.", example = "CODE", required = false)
    private String leiCode;

    /**
     * This property contains the URL of the Organization logo.
     **/
    @Schema(name = "logo", description = "This property contains the URL of the Organization logo.", example = "http://urltologo", required = false)
    private String logo;

    /**
     * This property refers to an Organization to which this Organization belongs.
     */
    @Schema(name = "memberOf", description = "This property refers to an Organization to which this Organization belongs.", example = "[{\n" +
            "    \"entityType\": \"ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> memberOf;

    /**
     * This property refers to a Facility or Equipment of which it is the owner.
     **/
    @Schema(name = "owns", description = "This property refers to a Facility or Equipment of which it is the owner.", example = "[{\n" +
            "    \"entityType\": \"EQUIPMENT or FACILITY\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> owns;


    /**
     * This property contains the telephone number of the Organization.
     **/
    @Schema(name = "telephone", description = "This property contains the telephone number of the Organization.", example = "[\"+003912414324\"]", required = false)
    private List<String> telephone;

    /**
     * This property contains the URL of the Organization website.
     **/
    @Schema(name = "URL", description = "This property contains the URL of the Organization website.", example = "http://urltowebsite", required = false)
    private String URL;

    /**
     * This property refers to the type of Organization (e.g., TCS, TCS internal
     * consortia, independent (INTERMAGNET), national EPOS consortia). A
     * controlled list for the values should be established.
     */
    @Schema(name = "type", description = "This property refers to the type of Organization (e.g., TCS, TCS internal consortia, independent (INTERMAGNET), national EPOS consortia). A controlled list for the values should be established.", example = "TCS", required = false)
    private String type;

    /**
     * This property contains information about the status of the Organization with
     * respect to EPOS (e.g., existing TCS, candidate TCS, external service). A
     * controlled list for the values should be established.
     */
    @Schema(name = "maturity", description = "TThis property contains information about the status of the Organization with respect to EPOS (e.g., existing TCS, candidate TCS, external service). A controlled list for the values should be established.", example = "some maturity value", required = false)
    private String maturity;


    public void addIdentifier(LinkedEntity identifier) {
        if (this.getIdentifier() == null) {
            ArrayList<LinkedEntity> identifierList = new ArrayList<>();
            identifierList.add(identifier);
            this.setIdentifier(identifierList);
        } else {
            this.getIdentifier().add(identifier);
        }
    }

    public void addEmail(String email) {
        if (this.getEmail() == null) {
            ArrayList<String> emailList = new ArrayList<>();
            emailList.add(email);
            this.setEmail(emailList);
        } else {
            this.getEmail().add(email);
        }
    }

    public void addTelephone(String telephone) {
        if (this.getTelephone() == null) {
            ArrayList<String> telephoneList = new ArrayList<>();
            telephoneList.add(telephone);
            this.setTelephone(telephoneList);
        } else {
            this.getTelephone().add(telephone);
        }
    }

    public void addLegalName(String legalName) {
        if (this.getLegalName() == null) {
            ArrayList<String> legalNameList = new ArrayList<>();
            legalNameList.add(legalName);
            this.setLegalName(legalNameList);
        } else {
            this.getLegalName().add(legalName);
        }
    }


    public void addContactPoint(LinkedEntity contactPoint) {
        if (this.getContactPoint() == null) {
            ArrayList<LinkedEntity> contactPointList = new ArrayList<>();
            contactPointList.add(contactPoint);
            this.setContactPoint(contactPointList);
        } else {
            this.getContactPoint().add(contactPoint);
        }
    }

    public void addMemberOf(LinkedEntity memberOf) {
        if (this.getMemberOf() == null) {
            ArrayList<LinkedEntity> memberOfList = new ArrayList<>();
            memberOfList.add(memberOf);
            this.setMemberOf(memberOfList);
        } else {
            this.getMemberOf().add(memberOf);
        }
    }


    public Organization address(LinkedEntity address) {
        this.address = address;
        return this;
    }

    /**
     * Get address
     *
     * @return address
     **/
    public LinkedEntity getAddress() {
        return address;
    }

    public void setAddress(LinkedEntity address) {
        this.address = address;
    }


    public Organization email(List<String> email) {
        this.email = email;
        return this;
    }

    public Organization addEmailItem(String emailItem) {
        if (this.email == null) {
            this.email = new ArrayList<>();
        }
        this.email.add(emailItem);
        return this;
    }

    /**
     * This property contains the email address of the Organization.
     *
     * @return email
     **/

    public List<String> getEmail() {
        return email;
    }

    public void setEmail(List<String> email) {
        this.email = email;
    }

    public Organization identifier(List<LinkedEntity> identifier) {
        this.identifier = identifier;
        return this;
    }

    public Organization addIdentifierItem(LinkedEntity identifierItem) {
        this.identifier.add(identifierItem);
        return this;
    }

    /**
     * This property contains an identifier for the Organization (e.g., PIC, ISNI, etc.).
     *
     * @return identifier
     **/
    public List<LinkedEntity> getIdentifier() {
        return identifier;
    }

    public void setIdentifier(List<LinkedEntity> identifier) {
        this.identifier = identifier;
    }

    public Organization legalName(List<String> legalName) {
        this.legalName = legalName;
        return this;
    }

    /**
     * This property contains the official name of the Organization.
     *
     * @return legalName
     **/

    public List<String> getLegalName() {
        return legalName;
    }

    public void setLegalName(List<String> legalName) {
        this.legalName = legalName;
    }

    public Organization leiCode(String leiCode) {
        this.leiCode = leiCode;
        return this;
    }

    /**
     * This property contains the Organization identifier that uniquely identifies a legal entity as defined in ISO 17442.
     *
     * @return leiCode
     **/

    public String getLeiCode() {
        return leiCode;
    }

    public void setLeiCode(String leiCode) {
        this.leiCode = leiCode;
    }

    public Organization logo(String logo) {
        this.logo = logo;
        return this;
    }

    /**
     * This property contains the URL of the Organization logo.
     *
     * @return logo
     **/

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }


    public Organization owns(List<LinkedEntity> ownedEquipments) {
        this.owns = owns;
        return this;
    }

    /**
     * This property refers to a Facility or Equipment of which it is the owner.
     *
     * @return owns
     **/

    public List<LinkedEntity> getOwns() {
        return owns;
    }

    public void setOwns(List<LinkedEntity> owns) {
        this.owns = owns;
    }

    public Organization addOwns(LinkedEntity owns) {
        if (this.owns == null) {
            this.owns = new ArrayList<>();
        }
        this.owns.add(owns);
        return this;
    }

    public void addOwns(String ownsString) {
        if (this.owns == null) {
            this.owns = new ArrayList<>();
        }
        LinkedEntity owns = new LinkedEntity();
        owns.setUid(ownsString);
        this.owns.add(owns);
    }

    public Organization telephone(List<String> telephone) {
        this.telephone = telephone;
        return this;
    }

    public Organization addTelephoneItem(String telephoneItem) {
        if (this.telephone == null) {
            this.telephone = new ArrayList<>();
        }
        this.telephone.add(telephoneItem);
        return this;
    }

    /**
     * This property contains the telephone number of the Organization.
     *
     * @return telephone
     **/

    public List<String> getTelephone() {
        return telephone;
    }

    public void setTelephone(List<String> telephone) {
        this.telephone = telephone;
    }

    public Organization URL(String URL) {
        this.URL = URL;
        return this;
    }

    /**
     * This property contains the URL of the Organization website.
     *
     * @return URL
     **/

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public List<LinkedEntity> getContactPoint() {
        return contactPoint;
    }

    public void setContactPoint(List<LinkedEntity> contactPoint) {
        this.contactPoint = contactPoint;
    }

    public List<LinkedEntity> getMemberOf() {
        return memberOf;
    }

    public void setMemberOf(List<LinkedEntity> memberOf) {
        this.memberOf = memberOf;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMaturity() {
        return maturity;
    }

    public void setMaturity(String maturity) {
        this.maturity = maturity;
    }

    @Override
    public String toString() {
        return "Organization{" +
                "address=" + address +
                ", contactPoint=" + contactPoint +
                ", email=" + email +
                ", identifier=" + identifier +
                ", acronym='" + acronym + '\'' +
                ", legalName=" + legalName +
                ", leiCode='" + leiCode + '\'' +
                ", logo='" + logo + '\'' +
                ", memberOf=" + memberOf +
                ", owns=" + owns +
                ", telephone=" + telephone +
                ", URL='" + URL + '\'' +
                ", type='" + type + '\'' +
                ", maturity='" + maturity + '\'' +
                '}'+ super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Organization that = (Organization) o;
        return Objects.equals(getAddress(), that.getAddress()) && Objects.equals(getContactPoint(), that.getContactPoint()) && Objects.equals(getEmail(), that.getEmail()) && Objects.equals(getIdentifier(), that.getIdentifier()) && Objects.equals(getAcronym(), that.getAcronym()) && Objects.equals(getLegalName(), that.getLegalName()) && Objects.equals(getLeiCode(), that.getLeiCode()) && Objects.equals(getLogo(), that.getLogo()) && Objects.equals(getMemberOf(), that.getMemberOf()) && Objects.equals(getOwns(), that.getOwns()) && Objects.equals(getTelephone(), that.getTelephone()) && Objects.equals(getURL(), that.getURL()) && Objects.equals(getType(), that.getType()) && Objects.equals(getMaturity(), that.getMaturity());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getAddress(), getContactPoint(), getEmail(), getIdentifier(), getAcronym(), getLegalName(), getLeiCode(), getLogo(), getMemberOf(), getOwns(), getTelephone(), getURL(), getType(), getMaturity());
    }
}