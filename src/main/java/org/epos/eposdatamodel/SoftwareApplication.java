package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents software package, application and program.
 */
public class SoftwareApplication extends EPOSDataModelEntity {
    /**
     * This property refers to a category of the Software Application. A Software Application may be associated with multiple categories.
     **/
    @Schema(name="category", description = "This property refers to a category of the Software Application. A Software Application may be associated with multiple categories.", example = "[{\n" +
            "    \"entityType\": \"CATEGORY\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> category;

    /**
     * This property refers to the Contact Point (i.e. Role) defined for the Software
     * Application.
     */
    @Schema(name="contactPoint", description = "This property refers to the Contact Point defined for the Software Application", example = "[{\n" +
            "    \"entityType\": \"CONTACTPOINT\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> contactPoint;

    /**
     * This property contains the description of the Software Application
     **/
    @Schema(name="description", description = "This property contains the description of the Software Application", example = "App description", required = false)
    private String description;

    /**
     * If the Software Application can be downloaded this property contains the URL to download it.
     **/
    @Schema(name="downloadURL", description = "If the Software Application can be downloaded this property contains the URL to download it.", example = "https://urltodownload", required = false)
    private String downloadURL;

    /**
     * This property contains an identifier for the Software Application.
     **/
    @Schema(name="identifier", description = "This property contains an identifier for the Software Application.", example = "[{\n" +
            "    \"entityType\": \"IDENTIFIER\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> identifier = new ArrayList<>();

    /**
     * This property contains the URL at which the application may be installed.
     **/
    @Schema(name="installURL", description = "This property contains the URL at which the application may be installed.", example = "https://urltoinstaller", required = false)
    private String installURL;

    /**
     * This property contains the keywords used to describe the Software Application. Multiple entries in a keywords list are typically delimited by commas.
     **/
    @Schema(name="keywords", description = "This property contains the keywords used to describe the Software Application. Multiple entries in a keywords list are typically delimited by commas.", example = "appkw1,appwk2", required = false)
    private String keywords;

    /**
     * This property contains the URL of the license document that applies to the Software Application.
     **/
    @Schema(name="licenseURL", description = "This property contains the URL of the license document that applies to the Software Application.", example = "https//githubrepo/LICENSE", required = false)
    private String licenseURL;

    /**
     * This property refers to the web page URL which describes the Software Application.
     **/
    @Schema(name="mainEntityOfPage", description = "This property refers to the web page URL which describes the Software Application.", example = "https//githubrepo/readme.md", required = false)
    private String mainEntityOfPage;

    /**
     * This property contains the name of the Software Application
     **/
    @Schema(name="name", description = "This property contains the name of the Software Application", example = "App name", required = false)
    private String name;

    /**
     * This property represents an input or output parameters of the Software Application.
     **/
    @Schema(name="parameter", description = "This property represents an input or output parameters of the Software Application.", example = "[{\n" +
            "    \"entityType\": \"PARAMETER\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> parameter;

    /**
     * It represents the link to another Epos resource. e.g. Software, WebService,
     * Operation linked to this software.
     */
    @Schema(name="relation", description = "It represents the link to OPERATION Epos resource", example = "[{\n" +
            "    \"entityType\": \"OPERATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> relation;

    /**
     * Component dependency requirements for application. This includes runtime environments and shared libraries that are not included in the application distribution package, but required to run the application.
     **/
    @Schema(name="requirements", description = "Component dependency requirements for application. This includes runtime environments and shared libraries that are not included in the application distribution package, but required to run the application.", example = "Java11", required = false)
    private String requirements;

    /**
     * This property contains the version of the Software Application instance.
     **/
    @Schema(name="softwareVersion", description = "This property contains the version of the Software Application instance.", example = "1.0", required = false)
    private String softwareVersion;

    public void addRelation(LinkedEntity relation) {
        if (this.getRelation() == null) {
            ArrayList<LinkedEntity> relationList = new ArrayList<>();
            relationList.add(relation);
            this.setRelation(relationList);
        } else {
            this.getRelation().add(relation);
        }
    }

    public void addCategory(LinkedEntity category) {
        if (this.getCategory() == null) {
            ArrayList<LinkedEntity> categoryList = new ArrayList<>();
            categoryList.add(category);
            this.setCategory(categoryList);
        } else {
            this.getCategory().add(category);
        }
    }

    public void addIdentifier(LinkedEntity identifier) {
        if (this.getIdentifier() == null) {
            ArrayList<LinkedEntity> identifierList = new ArrayList<>();
            identifierList.add(identifier);
            this.setIdentifier(identifierList);
        } else {
            this.getIdentifier().add(identifier);
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

    public void addParameter(LinkedEntity parameter) {
        if (this.getParameter() == null) {
            ArrayList<LinkedEntity> parameterList = new ArrayList<>();
            parameterList.add(parameter);
            this.setParameter(parameterList);
        } else {
            this.getParameter().add(parameter);
        }
    }


    public SoftwareApplication category(List<LinkedEntity> category) {
        this.category = category;
        return this;
    }

    public SoftwareApplication addCategoryItem(LinkedEntity categoryItem) {
        if (this.category == null) {
            this.category = new ArrayList<>();
        }
        this.category.add(categoryItem);
        return this;
    }

    /**
     * This property refers to a category of the Software Application. A Software Application may be associated with multiple categories.
     *
     * @return category
     **/

    public List<LinkedEntity> getCategory() {
        return category;
    }

    public void setCategory(List<LinkedEntity> category) {
        this.category = category;
    }

    public SoftwareApplication description(String description) {
        this.description = description;
        return this;
    }

    /**
     * This property contains the description of the Software Application
     *
     * @return description
     **/

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SoftwareApplication downloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
        return this;
    }

    /**
     * If the Software Application can be downloaded this property contains the URL to download it.
     *
     * @return downloadURL
     **/

    public String getDownloadURL() {
        return downloadURL;
    }

    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    public SoftwareApplication identifier(List<LinkedEntity> identifier) {
        this.identifier = identifier;
        return this;
    }

    public SoftwareApplication addIdentifierItem(LinkedEntity identifierItem) {
        this.identifier.add(identifierItem);
        return this;
    }

    /**
     * This property contains an identifier for the Software Application.
     *
     * @return identifier
     **/

    public List<LinkedEntity> getIdentifier() {
        return identifier;
    }

    public void setIdentifier(List<LinkedEntity> identifier) {
        this.identifier = identifier;
    }

    public SoftwareApplication installURL(String installURL) {
        this.installURL = installURL;
        return this;
    }

    /**
     * This property contains the URL at which the application may be installed.
     *
     * @return installURL
     **/

    public String getInstallURL() {
        return installURL;
    }

    public void setInstallURL(String installURL) {
        this.installURL = installURL;
    }

    public SoftwareApplication keywords(String keywords) {
        this.keywords = keywords;
        return this;
    }

    /**
     * This property contains the keywords used to describe the Software Application. Multiple entries in a keywords list are typically delimited by commas.
     *
     * @return keywords
     **/

    public String getKeywords() {
        return keywords;
    }

    public void addKeywords(String keyword) {
        if (this.keywords == null) {
            this.keywords = keyword;
        } else {
            this.keywords = this.keywords + ",\t" + keyword;
        }
    }


    public SoftwareApplication licenseURL(String licenseURL) {
        this.licenseURL = licenseURL;
        return this;
    }

    /**
     * This property contains the URL of the license document that applies to the Software Application.
     *
     * @return licenseURL
     **/

    public String getLicenseURL() {
        return licenseURL;
    }

    public void setLicenseURL(String licenseURL) {
        this.licenseURL = licenseURL;
    }

    public SoftwareApplication mainEntityOfPage(String mainEntityOfPage) {
        this.mainEntityOfPage = mainEntityOfPage;
        return this;
    }

    /**
     * This property refers to the web page URL which describes the Software Application.
     *
     * @return mainEntityofPage
     **/

    public String getMainEntityOfPage() {
        return mainEntityOfPage;
    }

    public void setMainEntityOfPage(String mainEntityOfPage) {
        this.mainEntityOfPage = mainEntityOfPage;
    }

    public SoftwareApplication name(String name) {
        this.name = name;
        return this;
    }

    /**
     * This property contains the name of the Software Application
     *
     * @return name
     **/

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SoftwareApplication parameter(ArrayList<LinkedEntity> parameter) {
        this.parameter = parameter;
        return this;
    }

    public SoftwareApplication addParameterItem(LinkedEntity parameterItem) {
        if (this.parameter == null) {
            this.parameter = new ArrayList<>();
        }
        this.parameter.add(parameterItem);
        return this;
    }

    /**
     * This property represents an input or output parameters of the Software Application.
     *
     * @return parameter
     **/

    public List<LinkedEntity> getParameter() {
        return parameter;
    }

    public void setParameter(List<LinkedEntity> parameter) {
        this.parameter = parameter;
    }


    public SoftwareApplication requirements(String requirements) {
        this.requirements = requirements;
        return this;
    }

    /**
     * Component dependency requirements for application. This includes runtime environments and shared libraries that are not included in the application distribution package, but required to run the application.
     *
     * @return requirements
     **/

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public SoftwareApplication softwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
        return this;
    }

    /**
     * This property contains the version of the Software Application instance.
     *
     * @return softwareVersion
     **/

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public List<LinkedEntity> getContactPoint() {
        return contactPoint;
    }

    public void setContactPoint(List<LinkedEntity> contactPoint) {
        this.contactPoint = contactPoint;
    }

    public List<LinkedEntity> getRelation() {
        return relation;
    }

    public void setRelation(List<LinkedEntity> relation) {
        this.relation = relation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SoftwareApplication that = (SoftwareApplication) o;
        return Objects.equals(category, that.category) && Objects.equals(contactPoint, that.contactPoint) && Objects.equals(description, that.description) && Objects.equals(downloadURL, that.downloadURL) && Objects.equals(identifier, that.identifier) && Objects.equals(installURL, that.installURL) && Objects.equals(keywords, that.keywords) && Objects.equals(licenseURL, that.licenseURL) && Objects.equals(mainEntityOfPage, that.mainEntityOfPage) && Objects.equals(name, that.name) && Objects.equals(parameter, that.parameter) && Objects.equals(relation, that.relation) && Objects.equals(requirements, that.requirements) && Objects.equals(softwareVersion, that.softwareVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, contactPoint, description, downloadURL, identifier, installURL, keywords, licenseURL, mainEntityOfPage, name, parameter, relation, requirements, softwareVersion);
    }

    @Override
    public String toString() {
        return "SoftwareApplication{" +
                "category=" + category +
                ", contactPoint=" + contactPoint +
                ", description='" + description + '\'' +
                ", downloadURL='" + downloadURL + '\'' +
                ", identifier=" + identifier +
                ", installURL='" + installURL + '\'' +
                ", keywords='" + keywords + '\'' +
                ", licenseURL='" + licenseURL + '\'' +
                ", mainEntityOfPage='" + mainEntityOfPage + '\'' +
                ", name='" + name + '\'' +
                ", parameter=" + parameter +
                ", relation=" + relation +
                ", requirements='" + requirements + '\'' +
                ", softwareVersion='" + softwareVersion + '\'' +
                '}'+ super.toString();
    }
}
