package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;

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
    @Schema(name="parameter", description = "This property represents a input parameters of the Software Application.", example = "[{\n" +
            "    \"entityType\": \"PARAMETER\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> inputParameter;

    /**
     * This property represents an input or output parameters of the Software Application.
     **/
    @Schema(name="parameter", description = "This property represents a output parameters of the Software Application.", example = "[{\n" +
            "    \"entityType\": \"PARAMETER\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> outputParameter;

    /**
     * It represents the link to another Epos resource. e.g. Software, WebService,
     * Operation linked to this software.
     */
    @Schema(name="relatedOperation", description = "It represents the link to OPERATION Epos resource", example = "[{\n" +
            "    \"entityType\": \"OPERATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> relatedOperation;

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

    @Schema(name="softwareStatus", description = "", required = false)
    private String softwareStatus;

    @Schema(name="spatial", description = "", required = false)
    private String spatial;

    @Schema(name="temporal", description = "", required = false)
    private String temporal;

    @Schema(name="fileSize", description = "", required = false)
    private String fileSize;

    @Schema(name="timeRequired", description = "", required = false)
    private String timeRequired;

    @Schema(name="processorRequirements", description = "", required = false)
    private String processorRequirements;

    @Schema(name="memoryrequirements", description = "", required = false)
    private String memoryrequirements;

    @Schema(name="storageRequirements", description = "", required = false)
    private String storageRequirements;

    @Schema(name="citation", description = "", required = false)
    private List<String> citation;

    @Schema(name="operatingSystem", description = "", required = false)
    private List<String> operatingSystem;

    /**
     * This property refers to a Person or Organization.
     **/
    @Schema(name = "author", description = "This property refers to a Person or Organization.", example = "[{\n" +
            "    \"entityType\": \"PERSON or ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> author;

    /**
     * This property refers to a Person or Organization.
     **/
    @Schema(name = "contributor", description = "This property refers to a Person or Organization.", example = "[{\n" +
            "    \"entityType\": \"PERSON or ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> contributor;

    /**
     * This property refers to a Person or Organization.
     **/
    @Schema(name = "funder", description = "This property refers to a Person or Organization.", example = "[{\n" +
            "    \"entityType\": \"PERSON or ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> funder;

    /**
     * This property refers to a Person or Organization.
     **/
    @Schema(name = "maintainer", description = "This property refers to a Person or Organization.", example = "[{\n" +
            "    \"entityType\": \"PERSON or ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> maintainer;

    /**
     * This property refers to a Person or Organization.
     **/
    @Schema(name = "provider", description = "This property refers to a Person or Organization.", example = "[{\n" +
            "    \"entityType\": \"PERSON or ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> provider;

    /**
     * This property refers to a Person or Organization.
     **/
    @Schema(name = "publisher", description = "This property refers to a Person or Organization.", example = "[{\n" +
            "    \"entityType\": \"PERSON or ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> publisher;

    /**
     * This property refers to a Person or Organization.
     **/
    @Schema(name = "creator", description = "This property refers to a Person or Organization.", example = "[{\n" +
            "    \"entityType\": \"PERSON or ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> creator;



    public void addRelatedOperation(LinkedEntity relation) {
        if (this.getRelatedOperation() == null) {
            ArrayList<LinkedEntity> relationList = new ArrayList<>();
            relationList.add(relation);
            this.setRelatedOperation(relationList);
        } else {
            this.getRelatedOperation().add(relation);
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

    public void addInputParameter(LinkedEntity parameter) {
        if (this.getInputParameter() == null) {
            ArrayList<LinkedEntity> parameterList = new ArrayList<>();
            parameterList.add(parameter);
            this.setInputParameter(parameterList);
        } else {
            this.getInputParameter().add(parameter);
        }
    }

    public void addCitation(String citation) {
        if (this.getCitation() == null) {
            ArrayList<String> citationList = new ArrayList<>();
            citationList.add(citation);
            this.setCitation(citationList);
        } else {
            this.getCitation().add(citation);
        }
    }

    public void addOperatingSystem(String operatingSystem) {
        if (this.getOperatingSystem() == null) {
            ArrayList<String> operatingSystemList = new ArrayList<>();
            operatingSystemList.add(operatingSystem);
            this.setOperatingSystem(operatingSystemList);
        } else {
            this.getOperatingSystem().add(operatingSystem);
        }
    }

    public void addOutputParameter(LinkedEntity parameter) {
        if (this.getOutputParameter() == null) {
            ArrayList<LinkedEntity> parameterList = new ArrayList<>();
            parameterList.add(parameter);
            this.setOutputParameter(parameterList);
        } else {
            this.getOutputParameter().add(parameter);
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

    public SoftwareApplication inputParameter(ArrayList<LinkedEntity> parameter) {
        this.inputParameter = inputParameter;
        return this;
    }

    public SoftwareApplication addInputParameterItem(LinkedEntity parameterItem) {
        if (this.inputParameter == null) {
            this.inputParameter = new ArrayList<>();
        }
        this.inputParameter.add(parameterItem);
        return this;
    }

    /**
     * This property represents an input or output parameters of the Software Application.
     *
     * @return parameter
     **/

    public List<LinkedEntity> getInputParameter() {
        return inputParameter;
    }

    public void setInputParameter(List<LinkedEntity> parameter) {
        this.inputParameter = parameter;
    }

    public SoftwareApplication outputParameter(ArrayList<LinkedEntity> outputParameter) {
        this.outputParameter = outputParameter;
        return this;
    }

    public SoftwareApplication addOutputParameterItem(LinkedEntity parameterItem) {
        if (this.outputParameter == null) {
            this.outputParameter = new ArrayList<>();
        }
        this.outputParameter.add(parameterItem);
        return this;
    }

    /**
     * This property represents an input or output parameters of the Software Application.
     *
     * @return parameter
     **/

    public List<LinkedEntity> getOutputParameter() {
        return outputParameter;
    }

    public void setOutputParameter(List<LinkedEntity> parameter) {
        this.outputParameter = parameter;
    }


    public List<LinkedEntity> getParameter() {
        List<LinkedEntity> merge = new ArrayList<>();
        if(inputParameter!=null) merge.addAll(inputParameter);
        if(outputParameter!=null)  merge.addAll(outputParameter);
        return merge;
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

    public List<LinkedEntity> getRelatedOperation() {
        return relatedOperation;
    }

    public void setRelatedOperation(List<LinkedEntity> relation) {
        this.relatedOperation = relation;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getSoftwareStatus() {
        return softwareStatus;
    }

    public void setSoftwareStatus(String softwareStatus) {
        this.softwareStatus = softwareStatus;
    }

    public String getSpatial() {
        return spatial;
    }

    public void setSpatial(String spatial) {
        this.spatial = spatial;
    }

    public String getTemporal() {
        return temporal;
    }

    public void setTemporal(String temporal) {
        this.temporal = temporal;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getTimeRequired() {
        return timeRequired;
    }

    public void setTimeRequired(String timeRequired) {
        this.timeRequired = timeRequired;
    }

    public String getProcessorRequirements() {
        return processorRequirements;
    }

    public void setProcessorRequirements(String processorRequirements) {
        this.processorRequirements = processorRequirements;
    }

    public String getMemoryrequirements() {
        return memoryrequirements;
    }

    public void setMemoryrequirements(String memoryrequirements) {
        this.memoryrequirements = memoryrequirements;
    }

    public String getStorageRequirements() {
        return storageRequirements;
    }

    public void setStorageRequirements(String storageRequirements) {
        this.storageRequirements = storageRequirements;
    }

    public List<String> getCitation() {
        return citation;
    }

    public void setCitation(List<String> citation) {
        this.citation = citation;
    }

    public List<String> getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(List<String> operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public List<LinkedEntity> getAuthor() {
        return author;
    }

    public void setAuthor(List<LinkedEntity> author) {
        this.author = author;
    }

    public SoftwareApplication addAuthor(LinkedEntity author) {
        if (this.author == null) {
            this.author = new ArrayList<>();
        }
        this.author.add(author);
        return this;
    }

    public List<LinkedEntity> getContributor() {
        return contributor;
    }

    public void setContributor(List<LinkedEntity> contributor) {
        this.contributor = contributor;
    }

    public SoftwareApplication addContributor(LinkedEntity contributor) {
        if (this.contributor == null) {
            this.contributor = new ArrayList<>();
        }
        this.contributor.add(contributor);
        return this;
    }

    public List<LinkedEntity> getFunder() {
        return funder;
    }

    public void setFunder(List<LinkedEntity> funder) {
        this.funder = funder;
    }

    public SoftwareApplication addFunder(LinkedEntity funder) {
        if (this.funder == null) {
            this.funder = new ArrayList<>();
        }
        this.funder.add(funder);
        return this;
    }

    public List<LinkedEntity> getMaintainer() {
        return maintainer;
    }

    public void setMaintainer(List<LinkedEntity> maintainer) {
        this.maintainer = maintainer;
    }

    public SoftwareApplication addMaintainer(LinkedEntity maintainer) {
        if (this.maintainer == null) {
            this.maintainer = new ArrayList<>();
        }
        this.maintainer.add(maintainer);
        return this;
    }

    public List<LinkedEntity> getProvider() {
        return provider;
    }

    public void setProvider(List<LinkedEntity> provider) {
        this.provider = provider;
    }

    public SoftwareApplication addProvider(LinkedEntity provider) {
        if (this.provider == null) {
            this.provider = new ArrayList<>();
        }
        this.provider.add(provider);
        return this;
    }


    public List<LinkedEntity> getPublisher() {
        return publisher;
    }

    public void setPublisher(List<LinkedEntity> publisher) {
        this.publisher = publisher;
    }

    public SoftwareApplication addPublisher(LinkedEntity publisher) {
        if (this.publisher == null) {
            this.publisher = new ArrayList<>();
        }
        this.publisher.add(publisher);
        return this;
    }


    public List<LinkedEntity> getCreator() {
        return creator;
    }

    public void setCreator(List<LinkedEntity> creator) {
        this.creator = creator;
    }

    public SoftwareApplication addCreator(LinkedEntity creator) {
        if (this.creator == null) {
            this.creator = new ArrayList<>();
        }
        this.creator.add(creator);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SoftwareApplication that = (SoftwareApplication) o;
        return Objects.equals(category, that.category) && Objects.equals(contactPoint, that.contactPoint) && Objects.equals(description, that.description) && Objects.equals(downloadURL, that.downloadURL) && Objects.equals(identifier, that.identifier) && Objects.equals(installURL, that.installURL) && Objects.equals(keywords, that.keywords) && Objects.equals(licenseURL, that.licenseURL) && Objects.equals(mainEntityOfPage, that.mainEntityOfPage) && Objects.equals(name, that.name) && Objects.equals(inputParameter, that.inputParameter) && Objects.equals(outputParameter, that.outputParameter) && Objects.equals(relatedOperation, that.relatedOperation) && Objects.equals(requirements, that.requirements) && Objects.equals(softwareVersion, that.softwareVersion) && Objects.equals(softwareStatus, that.softwareStatus) && Objects.equals(spatial, that.spatial) && Objects.equals(temporal, that.temporal) && Objects.equals(fileSize, that.fileSize) && Objects.equals(timeRequired, that.timeRequired) && Objects.equals(processorRequirements, that.processorRequirements) && Objects.equals(memoryrequirements, that.memoryrequirements) && Objects.equals(storageRequirements, that.storageRequirements) && Objects.equals(citation, that.citation) && Objects.equals(operatingSystem, that.operatingSystem) && Objects.equals(author, that.author) && Objects.equals(contributor, that.contributor) && Objects.equals(funder, that.funder) && Objects.equals(maintainer, that.maintainer) && Objects.equals(provider, that.provider) && Objects.equals(publisher, that.publisher) && Objects.equals(creator, that.creator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), category, contactPoint, description, downloadURL, identifier, installURL, keywords, licenseURL, mainEntityOfPage, name, inputParameter, outputParameter, relatedOperation, requirements, softwareVersion, softwareStatus, spatial, temporal, fileSize, timeRequired, processorRequirements, memoryrequirements, storageRequirements, citation, operatingSystem, author, contributor, funder, maintainer, provider, publisher, creator);
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
                ", inputParameter=" + inputParameter +
                ", outputParameter=" + outputParameter +
                ", relatedOperation=" + relatedOperation +
                ", requirements='" + requirements + '\'' +
                ", softwareVersion='" + softwareVersion + '\'' +
                ", softwareStatus='" + softwareStatus + '\'' +
                ", spatial='" + spatial + '\'' +
                ", temporal='" + temporal + '\'' +
                ", fileSize='" + fileSize + '\'' +
                ", timeRequired='" + timeRequired + '\'' +
                ", processorRequirements='" + processorRequirements + '\'' +
                ", memoryrequirements='" + memoryrequirements + '\'' +
                ", storageRequirements='" + storageRequirements + '\'' +
                ", citation=" + citation +
                ", operatingSystem=" + operatingSystem +
                ", author=" + author +
                ", contributor=" + contributor +
                ", funder=" + funder +
                ", maintainer=" + maintainer +
                ", provider=" + provider +
                ", publisher=" + publisher +
                ", creator=" + creator +
                '}';
    }
}
