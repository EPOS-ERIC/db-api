package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.persistence.internal.jpa.rs.metadata.model.Link;
import utilities.ParseLocalDateTime;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * It represents Data and Data Product.
 */
public class DataProduct extends EPOSDataModelEntity {

    /**
     * This property refers to information that indicates whether the Data Product is open data, has access restrictions or is not public.
     **/
    @Schema(name = "accessRight", description = "This property refers to information that indicates whether the Data Product is open data, has access restrictions or is not public.", example = "open data", required = false)
    private String accessRight;

    /**
     * This property refers to the frequency at which the Data Product is updated.
     **/
    @Schema(name = "accrualPeriodicity", description = "This property refers to the frequency at which the Data Product is updated.", example = "daily", required = false)
    private String accrualPeriodicity;

    /**
     * This property refers to a category of the Data Product. A Data Product may be associated with multiple categories.
     **/
    @Schema(name = "category", description = "This property refers to a category of the Data Product. A Data Product may be associated with multiple categories.", example = "[{\n" +
            "    \"entityType\": \"CATEGORY\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> category;

    /**
     * This property contains contact information (i.e. Role) that can be used for sending comments about the Data Product.
     */
    @Schema(name = "contactPoint", description = "This property contains contact information (i.e. Role) that can be used for sending comments about the Data Product.", example = "[{\n" +
            "    \"entityType\": \"CONTACTPOINT\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> contactPoint;

    /**
     * This property contains the date on which the Data Product was created.
     **/
    @Schema(name = "created", description = "This property contains the date on which the Data Product was created.", example = "2024-07-03T00:00:00", required = false)
    private LocalDateTime created;

    /**
     * This property contains the single identifier of the DataProduct
     */
    @Schema(name = "identifier", description = "This property contains the single identifier of the DataProduct", example = "[{\n" +
            "    \"entityType\": \"IDENTIFIER\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> identifier;

    /**
     * This property contains a free-text account of the Data Product. This property can be repeated for parallel language versions of the description.
     **/
    @Schema(name = "description", description = "This property contains a free-text account of the Data Product. This property can be repeated for parallel language versions of the description.", example = "[\"Example description text\"]", required = false)
    private List<String> description = new ArrayList<>();

    /**
     * This property links the Data Product to an available Distribution.
     */
    @Schema(name = "distribution", description = "This property links the Data Product to an available Distribution.", example = "[{\n" +
            "    \"entityType\": \"DISTRIBUTION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> distribution;

    /**
     * This property refers to a related Data Product that is part of the described Data Product.
     */
    @Schema(name = "hasPart", description = "This property refers to a related Data Product that is part of the described Data Product.", example = "[{\n" +
            "    \"entityType\": \"DATAPRODUCT\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> hasPart;

    /**
     * This property refers to a related Data Product in which the described Data Product is physically or logically included.
     */
    @Schema(name = "isPartOf", description = "This property refers to a related Data Product in which the described Data Product is physically or logically included.", example = "[{\n" +
            "    \"entityType\": \"DATAPRODUCT\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> isPartOf;

    /**
     * This property contains the date of formal issuance (e.g., publication) of the Data Product.
     **/
    @Schema(name = "issued", description = "This property contains the date of formal issuance (e.g., publication) of the Data Product.", example = "2024-07-03T00:00:00", required = false)
    private LocalDateTime issued;

    /**
     * This property contains a keyword or tag describing the Data Product. Multiple entries in a keywords list are typically delimited by commas.
     **/
    @Schema(name = "keywords", description = "This property contains a keyword or tag describing the Data Product. Multiple entries in a keywords list are typically delimited by commas.", example = "event,seismology,station", required = false)
    private String keywords;

    /**
     * This property contains the most recent date on which the Data Product was changed or modified.
     **/
    @Schema(name = "modified", description = "This property contains the most recent date on which the Data Product was changed or modified.", example = "2024-07-03T00:00:00", required = false)
    private LocalDateTime modified;

    /**
     * This property contains a statement about the lineage of a Data Product
     **/
    @Schema(name = "provenance", description = "This property contains a statement about the lineage of a Data Product", example = "[\"some provenance url\"]", required = false)
    private List<String> provenance;

    /**
     * This property refers to an entity (organization) responsible for making the Data Product available.
     */
    @Schema(name = "publisher", description = "This property refers to an entity (organization) responsible for making the Data Product available.", example = "[{\n" +
            "    \"entityType\": \"ORGANIZATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> publisher;

    /**
     * It represents the link to another Epos resource.
     */
    @Schema(name = "relation", description = "It represents the link to another Epos resource.", example = "[{\n" +
            "    \"entityType\": \"ANY ENTITY TYPE\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> relation;

    /**
     * This property refers to a geographic region that is covered by the Data Product. Linked entity of type LOCATION
     **/
    @Schema(name = "spatialExtent", description = "This property refers to a geographic region that is covered by the Data Product.", example = "[{\n" +
            "    \"entityType\": \"LOCATION\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> spatialExtent;

    /**
     * This property refers to a temporal period (i.e. startDate, endDate) that the Data Product covers. Linked entity of type PERIODOFTIME
     **/
    @Schema(name = "temporalExtent", description = "This property refers to a temporal period (i.e. startDate, endDate) that the Data Product covers.", example = "[{\n" +
            "    \"entityType\": \"PERIODOFTIME\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> temporalExtent;

    /**
     * This property contains a name given to the Data Product. This property can be repeated for parallel language versions of the name.
     **/
    @Schema(name = "title", description = "This property contains a name given to the Data Product. This property can be repeated for parallel language versions of the name.", example = "[\"Some DataProduct name\"]", required = false)
    private List<String> title = new ArrayList<>();

    /**
     * This property refers to the type of the Data Product. A controlled vocabulary for the values has not been established.
     **/
    @Schema(name = "type", description = "This property refers to the type of the Data Product. A controlled vocabulary for the values has not been established.", example = "http://purl.org/dc/dcmitype/Collection", required = false)
    private String type;

    /**
     * This property contains a version number or other version designation of the Data Product.
     **/
    @Schema(name = "versionInfo", description = "This property contains a version number or other version designation of the Data Product.", example = "1.0", required = false)
    private String versionInfo;

    /**
     * This property refers to the Data Product documentation.
     */
    @Schema(name = "documentation", description = "This property refers to the Data Product documentation.", example = "URL to documentation", required = false)
    private String documentation;

    /**
     * This property refers to the URI which make available information about quality assurance of the Data Product.
     */
    @Schema(name = "qualityAssurance", description = "This property refers to the URI which make available information about quality assurance of the Data Product.", example = "URL to quality assurance", required = false)
    private String qualityAssurance;

    /**
     * probabily an useless property
     */
    @Schema(name = "hasQualityAnnotation", description = "This property refers to the URI which make available information about quality assurance of the Data Product.", example = "URL to quality assurance", required = false)
    private String hasQualityAnnotation;

    public void addSpatialExtent(LinkedEntity spatialExtent) {
        if (this.getSpatialExtent() == null) {
            ArrayList<LinkedEntity> spatialExtentList = new ArrayList<>();
            spatialExtentList.add(spatialExtent);
            this.setSpatialExtent(spatialExtentList);
        } else {
            this.getSpatialExtent().add(spatialExtent);
        }
    }

    public void addTitle(String title) {
        if (this.getTitle() == null) {
            ArrayList<String> titleList = new ArrayList<>();
            titleList.add(title);
            this.setTitle(titleList);
        } else {
            this.getTitle().add(title);
        }
    }

    public void addDescription(String description) {
        if (this.getDescription() == null) {
            ArrayList<String> descriptionList = new ArrayList<>();
            descriptionList.add(description);
            this.setDescription(descriptionList);
        } else {
            this.getDescription().add(description);
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

    public void addPublisher(LinkedEntity publisher) {
        if (this.getPublisher() == null) {
            ArrayList<LinkedEntity> publisherList = new ArrayList<>();
            publisherList.add(publisher);
            this.setPublisher(publisherList);
        } else {
            this.getPublisher().add(publisher);
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

    public void addDistribution(LinkedEntity distribution) {
        if (this.getDistribution() == null) {
            ArrayList<LinkedEntity> distributionList = new ArrayList<>();
            distributionList.add(distribution);
            this.setDistribution(distributionList);
        } else {
            this.getDistribution().add(distribution);
        }
    }

    public void addIsPartOf(LinkedEntity isPartOf) {
        if (this.getIsPartOf() == null) {
            ArrayList<LinkedEntity> isPartOfList = new ArrayList<>();
            isPartOfList.add(isPartOf);
            this.setIsPartOf(isPartOfList);
        } else {
            this.getIsPartOf().add(isPartOf);
        }
    }

    public void addHasPart(LinkedEntity hasPart) {
        if (this.getHasPart() == null) {
            ArrayList<LinkedEntity> hasPartList = new ArrayList<>();
            hasPartList.add(hasPart);
            this.setHasPart(hasPartList);
        } else {
            this.getHasPart().add(hasPart);
        }
    }

    public void addProvenance(String provenance) {
        if (this.getProvenance() == null) {
            ArrayList<String> provenanceList = new ArrayList<>();
            provenanceList.add(provenance);
            this.setProvenance(provenanceList);
        } else {
            this.getProvenance().add(provenance);
        }
    }
    public DataProduct accessRight(String accessRight) {
        this.accessRight = accessRight;
        return this;
    }

    /**
     * This property refers to information that indicates whether the Data Product is open data, has access restrictions or is not public.
     *
     * @return accessRight
     **/

    public String getAccessRight() {
        return accessRight;
    }

    public void setAccessRight(String accessRight) {
        this.accessRight = accessRight;
    }

    public DataProduct accrualPeriodicity(String accrualPeriodicity) {
        this.accrualPeriodicity = accrualPeriodicity;
        return this;
    }

    /**
     * This property refers to the frequency at which the Data Product is updated.
     *
     * @return accrualPeriodicity
     **/

    public String getAccrualPeriodicity() {
        return accrualPeriodicity;
    }

    public void setAccrualPeriodicity(String accrualPeriodicity) {
        this.accrualPeriodicity = accrualPeriodicity;
    }

    public DataProduct category(List<LinkedEntity> category) {
        this.category = category;
        return this;
    }

    public DataProduct addCategoryItem(LinkedEntity categoryItem) {
        if (this.category == null) {
            this.category = new ArrayList<>();
        }
        this.category.add(categoryItem);
        return this;
    }

    /**
     * This property refers to a category of the Data Product. A Data Product may be associated with multiple categories.
     *
     * @return category
     **/

    public List<LinkedEntity> getCategory() {
        return category;
    }

    public void setCategory(List<LinkedEntity> category) {
        this.category = category;
    }

    public DataProduct created(LocalDateTime created) {
        this.created = created;
        return this;
    }

    /**
     * This property contains the date on which the Data Product was created.
     *
     * @return created
     **/

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public void setCreated(String created){
        if(!created.isEmpty() && !created.isBlank())
            this.created = ParseLocalDateTime.parse(created);
    }

    public DataProduct description(List<String> description) {
        this.description = description;
        return this;
    }

    public DataProduct addDescriptionItem(String descriptionItem) {
        this.description.add(descriptionItem);
        return this;
    }

    /**
     * This property contains a free-text account of the Data Product. This property can be repeated for parallel language versions of the description.
     *
     * @return description
     **/

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }


    public DataProduct identifier(List<LinkedEntity> identifier) {
        this.identifier = identifier;
        return this;
    }

    public DataProduct addIdentifierItem(LinkedEntity identifierItem) {
        if (this.identifier == null) {
            this.identifier = new ArrayList<>();
        }
        this.identifier.add(identifierItem);
        return this;
    }

    /**
     * This property refers to a secondary identifier of the Data Product, such as MAST/ADS, DataCite, DOI, EZID or W3ID.
     *
     * @return identifier
     **/
    public List<LinkedEntity> getIdentifier() {
        return identifier;
    }

    public void setIdentifier(List<LinkedEntity> identifier) {
        this.identifier = identifier;
    }

    public DataProduct issued(LocalDateTime issued) {
        this.issued = issued;
        return this;
    }

    /**
     * This property contains the date of formal issuance (e.g., publication) of the Data Product.
     *
     * @return issued
     **/

    public LocalDateTime getIssued() {
        return issued;
    }

    public void setIssued(LocalDateTime issued) {
        this.issued = issued;
    }

    public void setIssued(String issued){
        if(!issued.isEmpty() && !issued.isBlank())
            this.issued = ParseLocalDateTime.parse(issued);
    }

    public DataProduct keywords(String keywords) {
        this.keywords = keywords;
        return this;
    }

    public void addKeywords(String keyword) {
        if (this.keywords == null) {
            this.keywords = keyword;
        } else {
            this.keywords = this.keywords + ",\t" + keyword;
        }
    }

    /**
     * This property contains a keyword or tag describing the Data Product. Multiple entries in a keywords list are typically delimited by commas.
     *
     * @return keywords
     **/

    public String getKeywords() {
        return keywords;
    }


    public DataProduct modified(LocalDateTime modified) {
        this.modified = modified;
        return this;
    }

    /**
     * This property contains the most recent date on which the Data Product was changed or modified.
     *
     * @return modified
     **/

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }

    public void setModified(String modified){
        if(!modified.isEmpty() && !modified.isBlank())
            this.modified = ParseLocalDateTime.parse(modified);
    }

    public DataProduct provenance(List<String> provenance) {
        this.provenance = provenance;
        return this;
    }

    public DataProduct addProvenanceItem(String provenanceItem) {
        if (this.provenance == null) {
            this.provenance = new ArrayList<>();
        }
        this.provenance.add(provenanceItem);
        return this;
    }

    /**
     * This property contains a statement about the lineage of a Data Product
     *
     * @return provenance
     **/

    public List<String> getProvenance() {
        return provenance;
    }

    public void setProvenance(List<String> provenance) {
        this.provenance = provenance;
    }


    public DataProduct spatialExtent(List<LinkedEntity> spatialExtent) {
        this.spatialExtent = spatialExtent;
        return this;
    }

    public DataProduct addSpatialExtentItem(LinkedEntity spatialExtentItem) {
        if (this.spatialExtent == null) {
            this.spatialExtent = new ArrayList<>();
        }
        this.spatialExtent.add(spatialExtentItem);
        return this;
    }

    /**
     * This property refers to a geographic region that is covered by the Data Product.
     *
     * @return spatialExtent
     **/
    public List<LinkedEntity> getSpatialExtent() {
        return spatialExtent;
    }

    public void setSpatialExtent(ArrayList<LinkedEntity> spatialExtent) {
        this.spatialExtent = spatialExtent;
    }

    public DataProduct temporalExtent(List<LinkedEntity> temporalExtent) {
        this.temporalExtent = temporalExtent;
        return this;
    }

    public DataProduct addTemporalExtentItem(LinkedEntity temporalExtentItem) {
        if (this.temporalExtent == null) {
            this.temporalExtent = new ArrayList<>();
        }
        this.temporalExtent.add(temporalExtentItem);
        return this;
    }

    /**
     * This property refers to a temporal period (i.e. startDate, endDate) that the Data Product covers.
     *
     * @return temporalExtent
     **/
    public List<LinkedEntity> getTemporalExtent() {
        return temporalExtent;
    }

    public void setTemporalExtent(List<LinkedEntity> temporalExtent) {
        this.temporalExtent = temporalExtent;
    }

    public DataProduct title(List<String> title) {
        this.title = title;
        return this;
    }

    public DataProduct addTitleItem(String titleItem) {
        this.title.add(titleItem);
        return this;
    }

    /**
     * This property contains a name given to the Data Product. This property can be repeated for parallel language versions of the name.
     *
     * @return title
     **/

    public List<String> getTitle() {
        return title;
    }

    public void setTitle(List<String> title) {
        this.title = title;
    }

    public DataProduct type(String type) {
        this.type = type;
        return this;
    }

    /**
     * This property refers to the type of the Data Product. A controlled vocabulary for the values has not been established.
     *
     * @return type
     **/

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DataProduct versionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
        return this;
    }

    /**
     * This property contains a version number or other version designation of the Data Product.
     *
     * @return versionInfo
     **/

    public String getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }

    public List<LinkedEntity> getContactPoint() {
        return contactPoint;
    }

    public void setContactPoint(List<LinkedEntity> contactPoint) {
        this.contactPoint = contactPoint;
    }

    public List<LinkedEntity> getDistribution() {
        return distribution;
    }

    public void setDistribution(List<LinkedEntity> distribution) {
        this.distribution = distribution;
    }

    public List<LinkedEntity> getHasPart() {
        return hasPart;
    }

    public void setHasPart(List<LinkedEntity> hasPart) {
        this.hasPart = hasPart;
    }

    public List<LinkedEntity> getIsPartOf() {
        return isPartOf;
    }

    public void setIsPartOf(List<LinkedEntity> isPartOf) {
        this.isPartOf = isPartOf;
    }

    public List<LinkedEntity> getPublisher() {
        return publisher;
    }

    public void setPublisher(List<LinkedEntity> publisher) {
        this.publisher = publisher;
    }

    public List<LinkedEntity> getRelation() {
        return relation;
    }

    public void setRelation(List<LinkedEntity> relation) {
        this.relation = relation;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public void setSpatialExtent(List<LinkedEntity> spatialExtent) {
        this.spatialExtent = spatialExtent;
    }

    public String getQualityAssurance() {
        return qualityAssurance;
    }

    public void setQualityAssurance(String qualityAssurance) {
        this.qualityAssurance = qualityAssurance;
    }

    public String getHasQualityAnnotation() {
        return hasQualityAnnotation;
    }

    public void setHasQualityAnnotation(String hasQualityAnnotation) {
        this.hasQualityAnnotation = hasQualityAnnotation;
    }

    @Override
    public String toString() {
        return "DataProduct{ accessRight='" + accessRight + '\'' +
                ", accrualPeriodicity='" + accrualPeriodicity + '\'' +
                ", category=" + category +
                ", contactPoint=" + contactPoint +
                ", created=" + created +
                ", description=" + description +
                ", distribution=" + distribution +
                ", hasPart=" + hasPart +
                ", identifier=" + identifier +
                ", isPartOf=" + isPartOf +
                ", issued=" + issued +
                ", keywords='" + keywords + '\'' +
                ", modified=" + modified +
                ", provenance=" + provenance +
                ", publisher=" + publisher +
                ", relation=" + relation +
                ", spatialExtent=" + spatialExtent +
                ", temporalExtent=" + temporalExtent +
                ", title=" + title +
                ", type='" + type + '\'' +
                ", versionInfo='" + versionInfo + '\'' +
                ", documentation='" + documentation + '\'' +
                ", qualityAssurance='" + qualityAssurance + '\'' +
                ", hasQualityAnnotation='" + hasQualityAnnotation + '\'' +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DataProduct that = (DataProduct) o;
        return Objects.equals(getAccessRight(), that.getAccessRight()) && Objects.equals(getAccrualPeriodicity(), that.getAccrualPeriodicity()) && Objects.equals(getCategory(), that.getCategory()) && Objects.equals(getContactPoint(), that.getContactPoint()) && Objects.equals(getCreated(), that.getCreated()) && Objects.equals(getDescription(), that.getDescription()) && Objects.equals(getDistribution(), that.getDistribution()) && Objects.equals(getHasPart(), that.getHasPart()) && Objects.equals(getIdentifier(), that.getIdentifier()) && Objects.equals(getIsPartOf(), that.getIsPartOf()) && Objects.equals(getIssued(), that.getIssued()) && Objects.equals(getKeywords(), that.getKeywords()) && Objects.equals(getModified(), that.getModified()) && Objects.equals(getProvenance(), that.getProvenance()) && Objects.equals(getPublisher(), that.getPublisher()) && Objects.equals(getRelation(), that.getRelation()) && Objects.equals(getSpatialExtent(), that.getSpatialExtent()) && Objects.equals(getTemporalExtent(), that.getTemporalExtent()) && Objects.equals(getTitle(), that.getTitle()) && Objects.equals(getType(), that.getType()) && Objects.equals(getVersionInfo(), that.getVersionInfo()) && Objects.equals(getDocumentation(), that.getDocumentation()) && Objects.equals(qualityAssurance, that.qualityAssurance) && Objects.equals(getHasQualityAnnotation(), that.getHasQualityAnnotation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getAccessRight(), getAccrualPeriodicity(), getCategory(), getContactPoint(), getCreated(), getDescription(), getDistribution(), getHasPart(), getIdentifier(), getIsPartOf(), getIssued(), getKeywords(), getModified(), getProvenance(), getPublisher(), getRelation(), getSpatialExtent(), getTemporalExtent(), getTitle(), getType(), getVersionInfo(), getDocumentation(), qualityAssurance, getHasQualityAnnotation());
    }
}
