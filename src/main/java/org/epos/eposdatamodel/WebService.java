package org.epos.eposdatamodel;


import io.swagger.v3.oas.annotations.media.Schema;
import utilities.ParseLocalDateTime;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Online interfaces (APIs) enabling a user or a machine to programmatically access the given resource.
 */
public class WebService extends EPOSDataModelEntity {

    /**
     * This property refers to a secondary identifier of the Web Service, such as MAST/ADS, DataCite, DOI, EZID or W3ID.
     **/
    @Schema(name = "identifier", description = "This property refers to identifiers of the Web Service, such as MAST/ADS, DataCite, DOI, EZID or W3ID.", example = "[{\n" +
            "    \"entityType\": \"IDENTIFIER\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> identifier;

    /**
     * This property contains the single idenfier of the WebService.
     */
    //private String schemaIdentifier;

    /**
     * This property refers to a category of the Web Service. A Web Service may be associated with multiple categories.
     **/
    @Schema(name = "category", description = "This property refers to a category of the Web Service. A Web Service may be associated with multiple categories.", example = "[{\n" +
            "    \"entityType\": \"CATEGORY\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> category;

    /**
     * This property contains contact information (i.e. Role) that can be used for
     * sending comments about the Web Service.
     */
    @Schema(name = "contactPoint", description = "This property contains contact information that can be used for sending comments about the Web Service.", example = "[{\n" +
            "    \"entityType\": \"CONTACTPOINT\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> contactPoint;

    /**
     * This property contains the most recent date on which the Web Service was modified.
     **/
    @Schema(name = "dateModified", description = "This property contains the most recent date on which the Web Service was modified.", example = "2024-07-03T00:00:00", required = false)
    private LocalDateTime dateModified;

    /**
     * This property contains the date of publication of the Web Service.
     **/
    @Schema(name = "datePublished", description = "This property contains the date of publication of the Web Service.", example = "2024-07-03T00:00:00", required = false)
    private LocalDateTime datePublished;

    /**
     * This property contains a free-text description of the Web Service.
     **/
    @Schema(name = "description", description = "This property contains a free-text description of the Web Service.", example = "Webservice description text", required = false)
    private String description;

    /**
     * This property refers to the API documentation.
     **/
    @Schema(name = "documentation", description = "This property refers to the API documentation.", example = "[{\n" +
            "    \"entityType\": \"DOCUMENTATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> documentation;

    /**
     * This property refers to the API definitions (e.g., WSDL, WADL)
     **/
    @Schema(name = "entryPoint", description = "This property refers to the API definitions (e.g., WSDL, WADL)", example = "https://entrypoint", required = false)
    private String entryPoint;

    /**
     * This property contains the keywords used to describe the Web Service. Multiple entries in a keywords list are typically delimited by commas.
     **/
    @Schema(name = "keywords", description = "This property contains the keywords used to describe the Web Service. Multiple entries in a keywords list are typically delimited by commas.", example = "kw1,kw2", required = false)
    private String keywords;

    /**
     * This property refers to the licence under which the Web Service can be used or reused.
     **/
    @Schema(name = "license", description = "This property refers to the licence under which the Web Service can be used or reused.", example = "Apache2", required = false)
    private String license;

    /**
     * This property contains a name given to the Web Service.
     **/
    @Schema(name = "name", description = "This property contains a name given to the Web Service.", example = "Webservice name", required = false)
    private String name;

    /**
     * This property refers to an Organisation responsible for making the Web Service available.
     **/
    @Schema(name = "provider", description = "This property refers to an Organisation responsible for making the Web Service available.", example = "{\n" +
            "    \"entityType\": \"ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }", required = false)
    private LinkedEntity provider;

    /**
     * This property refers to a geographical area covered by the Web Service.
     **/
    @Schema(name = "spatialExtent", description = "This property refers to a geographical area covered by the Web Service.", example = "[{\n" +
            "    \"entityType\": \"LOCATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> spatialExtent;

    /**
     * This property refers to a web service operation supported by the
     * Web Service.
     */
    @Schema(name="supportedOperation",description = "This property refers to a web service operation supported by the Web Service.", example = "[{\n" +
            "    \"entityType\": \"OPERATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> supportedOperation;

    /**
     * This property refers to a temporal period (i.e. startDate, endDate) that the Web Service covers.
     **/
    @Schema(name="temporalExtent", description = "This property refers to a temporal period (i.e. startDate, endDate) that the Web Service covers.", example = "[{\n" +
            "    \"entityType\": \"PERIODOFTIME\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> temporalExtent;

    /**
     * Reverse reference to the related distribution.
     */
    @Schema(name= "distribution", description = "Reverse reference to the related distribution.", example = "[{\n" +
            "    \"entityType\": \"DISTRIBUTION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> distribution;
    
    /**
     * It represents the link to another Epos resource.
     */
    @Schema(name= "webserviceRelation", description = "It represents the link to another Epos resource.", example = "{\n" +
            "    \"entityType\": \"ANY ENTITY\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }", required = false)
    private List<LinkedEntity> webserviceRelation;


    /**
     * List of possible authentication/authorization methods supported.
     */
    @Schema(name= "aaaiTypes", description = "List of possible authentication/authorization methods supported.", example = "OAUTH", required = false)
    private String aaaiTypes;
    

    public void addWebserviceRelation(LinkedEntity relation) {
        if (this.getWebserviceRelation() == null) {
            ArrayList<LinkedEntity> relationList = new ArrayList<>();
            relationList.add(relation);
            this.setWebserviceRelation(relationList);
        } else {
            this.getWebserviceRelation().add(relation);
        }
    }


    public void addSpatialExtent(LinkedEntity spatialExtent) {
        if (this.getSpatialExtent() == null) {
            ArrayList<LinkedEntity> spatialExtentList = new ArrayList<>();
            spatialExtentList.add(spatialExtent);
            this.setSpatialExtent(spatialExtentList);
        } else {
            this.getSpatialExtent().add(spatialExtent);
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

    public void addContactPoint(LinkedEntity contactPoint) {
        if (this.getContactPoint() == null) {
            ArrayList<LinkedEntity> contactPointList = new ArrayList<>();
            contactPointList.add(contactPoint);
            this.setContactPoint(contactPointList);
        } else {
            this.getContactPoint().add(contactPoint);
        }
    }

    public void addSupportedOperation(LinkedEntity supportedOperation) {
        if (this.getSupportedOperation() == null) {
            ArrayList<LinkedEntity> supportedOperationList = new ArrayList<>();
            supportedOperationList.add(supportedOperation);
            this.setSupportedOperation(supportedOperationList);
        } else {
            this.getSupportedOperation().add(supportedOperation);
        }
    }

    public void addDocumentation(LinkedEntity documentation) {
        if (this.getDocumentation() == null) {
            ArrayList<LinkedEntity> documentationList = new ArrayList<>();
            documentationList.add(documentation);
            this.setDocumentation(documentationList);
        } else {
            this.getDocumentation().add(documentation);
        }
    }


    public void addTemporalExtent(LinkedEntity temporalExtent) {
        if (this.getTemporalExtent() == null) {
            ArrayList<LinkedEntity> temporalExtentList = new ArrayList<>();
            temporalExtentList.add(temporalExtent);
            this.setTemporalExtent(temporalExtentList);
        } else {
            this.getTemporalExtent().add(temporalExtent);
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

    public WebService identifier(List<LinkedEntity> identifier) {
        this.identifier = identifier;
        return this;
    }

    public WebService addIdentifierItem(LinkedEntity identifierItem) {
        if (this.identifier == null) {
            this.identifier = new ArrayList<>();
        }
        this.identifier.add(identifierItem);
        return this;
    }

    /**
     * This property refers to a secondary identifier of the Web Service, such as MAST/ADS, DataCite, DOI, EZID or W3ID.
     *
     * @return identifier
     **/
    public List<LinkedEntity> getIdentifier() {
        return identifier;
    }

    public void setIdentifier(List<LinkedEntity> identifier) {
        this.identifier = identifier;
    }

    public WebService category(List<LinkedEntity> category) {
        this.category = category;
        return this;
    }

    public WebService addCategoryItem(LinkedEntity categoryItem) {
        if (this.category == null) {
            this.category = new ArrayList<>();
        }
        this.category.add(categoryItem);
        return this;
    }

    /**
     * This property refers to a category of the Web Service. A Web Service may be associated with multiple categories.
     *
     * @return category
     **/

    public List<LinkedEntity> getCategory() {
        return category;
    }

    public void setCategory(List<LinkedEntity> category) {
        this.category = category;
    }


    public WebService dateModified(LocalDateTime dateModified) {
        this.dateModified = dateModified;
        return this;
    }

    /**
     * This property contains the most recent date on which the Web Service was modified.
     *
     * @return dateModified
     **/

    public LocalDateTime getDateModified() {
        return dateModified;
    }

    public void setDateModified(LocalDateTime dateModified) {
        this.dateModified = dateModified;
    }

    public void setDateModified(String dateModified){
        if(!dateModified.isEmpty() && !dateModified.isBlank())
            this.dateModified = ParseLocalDateTime.parse(dateModified);
    }

    public WebService datePublished(LocalDateTime datePublished) {
        this.datePublished = datePublished;
        return this;
    }

    /**
     * This property contains the date of publication of the Web Service.
     *
     * @return datePublished
     **/

    public LocalDateTime getDatePublished() {
        return datePublished;
    }

    public void setDatePublished(LocalDateTime datePublished) {
        this.datePublished = datePublished;
    }

    public void setDatePublished(String datePublished){
        if(!datePublished.isEmpty() && !datePublished.isBlank())
            this.datePublished = ParseLocalDateTime.parse(datePublished);
    }


    public WebService description(String description) {
        this.description = description;
        return this;
    }

    /**
     * This property contains a free-text description of the Web Service.
     *
     * @return description
     **/

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WebService documentation(List<LinkedEntity> documentation) {
        this.documentation = documentation;
        return this;
    }

    public WebService addDocumentationItem(LinkedEntity documentationItem) {
        if (this.documentation == null) {
            this.documentation = new ArrayList<>();
        }
        this.documentation.add(documentationItem);
        return this;
    }

    /**
     * This property refers to the API documentation.
     *
     * @return documentation
     **/
    public List<LinkedEntity> getDocumentation() {
        return documentation;
    }

    public void setDocumentation(ArrayList<LinkedEntity> documentation) {
        this.documentation = documentation;
    }

    public WebService entryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
        return this;
    }

    /**
     * This property refers to the API definitions (e.g., WSDL, WADL)
     *
     * @return entryPoint
     **/

    public String getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public WebService keywords(String keywords) {
        this.keywords = keywords;
        return this;
    }

    /**
     * This property contains the keywords used to describe the Web Service. Multiple entries in a keywords list are typically delimited by commas.
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


    public WebService license(String license) {
        this.license = license;
        return this;
    }

    /**
     * This property refers to the licence under which the Web Service can be used or reused.
     *
     * @return license
     **/

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public WebService name(String name) {
        this.name = name;
        return this;
    }

    /**
     * This property contains a name given to the Web Service.
     *
     * @return name
     **/

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WebService provider(LinkedEntity provider) {
        this.provider = provider;
        return this;
    }

    /**
     * This property refers to an Organisation responsible for making the Web Service available.
     *
     * @return provider
     **/

    public LinkedEntity getProvider() {
        return provider;
    }

    public void setProvider(LinkedEntity provider) {
        this.provider = provider;
    }

    public WebService spatialExtent(List<LinkedEntity> spatialExtent) {
        this.spatialExtent = spatialExtent;
        return this;
    }

    public WebService addSpatialExtentItem(LinkedEntity spatialExtentItem) {
        if (this.spatialExtent == null) {
            this.spatialExtent = new ArrayList<>();
        }
        this.spatialExtent.add(spatialExtentItem);
        return this;
    }

    /**
     * This property refers to a geographical area covered by the Web Service.
     *
     * @return spatialExtent
     **/
    public List<LinkedEntity> getSpatialExtent() {
        return spatialExtent;
    }

    public void setSpatialExtent(ArrayList<LinkedEntity> spatialExtent) {
        this.spatialExtent = spatialExtent;
    }


    public WebService temporalExtent(List<LinkedEntity> temporalExtent) {
        this.temporalExtent = temporalExtent;
        return this;
    }

    public WebService addTemporalExtentItem(LinkedEntity temporalExtentItem) {
        if (this.temporalExtent == null) {
            this.temporalExtent = new ArrayList<>();
        }
        this.temporalExtent.add(temporalExtentItem);
        return this;
    }

    /**
     * This property refers to a temporal period (i.e. startDate, endDate) that the Web Service covers.
     *
     * @return temporalExtent
     **/
    public List<LinkedEntity> getTemporalExtent() {
        return temporalExtent;
    }

    public void setTemporalExtent(List<LinkedEntity> temporalExtent) {
        this.temporalExtent = temporalExtent;
    }

    public List<LinkedEntity> getContactPoint() {
        return contactPoint;
    }

    public void setContactPoint(List<LinkedEntity> contactPoint) {
        this.contactPoint = contactPoint;
    }

    public List<LinkedEntity> getSupportedOperation() {
        return supportedOperation;
    }

    public void setSupportedOperation(List<LinkedEntity> supportedOperation) {
        this.supportedOperation = supportedOperation;
    }

    public String getAaaiTypes() {
        return aaaiTypes;
    }

    public void setAaaiTypes(String aaaiTypes) {
        this.aaaiTypes = aaaiTypes;
    }

    public List<LinkedEntity> getDistribution() {
        if(distribution == null) distribution = new ArrayList<>();
        return distribution;
    }

    public WebService setDistribution(List<LinkedEntity> distribution) {
        this.distribution = distribution;
        return this;
    }
    
    public List<LinkedEntity> getWebserviceRelation() {
        return webserviceRelation;
    }

    public void setWebserviceRelation(List<LinkedEntity> webserviceRelation) {
        this.webserviceRelation = webserviceRelation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WebService that = (WebService) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(category, that.category) && Objects.equals(contactPoint, that.contactPoint) && Objects.equals(dateModified, that.dateModified) && Objects.equals(datePublished, that.datePublished) && Objects.equals(description, that.description) && Objects.equals(documentation, that.documentation) && Objects.equals(entryPoint, that.entryPoint) && Objects.equals(keywords, that.keywords) && Objects.equals(license, that.license) && Objects.equals(name, that.name) && Objects.equals(provider, that.provider) && Objects.equals(spatialExtent, that.spatialExtent) && Objects.equals(supportedOperation, that.supportedOperation) && Objects.equals(temporalExtent, that.temporalExtent) && Objects.equals(distribution, that.distribution) && Objects.equals(webserviceRelation, that.webserviceRelation) && Objects.equals(aaaiTypes, that.aaaiTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), identifier, category, contactPoint, dateModified, datePublished, description, documentation, entryPoint, keywords, license, name, provider, spatialExtent, supportedOperation, temporalExtent, distribution, webserviceRelation, aaaiTypes);
    }

    @Override
    public String toString() {
        return "WebService{" +
                "identifier=" + identifier +
                ", category=" + category +
                ", contactPoint=" + contactPoint +
                ", dateModified=" + dateModified +
                ", datePublished=" + datePublished +
                ", description='" + description + '\'' +
                ", documentation=" + documentation +
                ", entryPoint='" + entryPoint + '\'' +
                ", keywords='" + keywords + '\'' +
                ", license='" + license + '\'' +
                ", name='" + name + '\'' +
                ", provider=" + provider +
                ", spatialExtent=" + spatialExtent +
                ", supportedOperation=" + supportedOperation +
                ", temporalExtent=" + temporalExtent +
                ", distribution=" + distribution +
                ", webserviceRelation=" + webserviceRelation +
                ", aaaiTypes='" + aaaiTypes + '\'' +
                '}'+ super.toString();
    }
}
