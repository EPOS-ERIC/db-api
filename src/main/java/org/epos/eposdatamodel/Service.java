package org.epos.eposdatamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes an offer of an RI to the community. It could be free or ask for something in return (money,
 * publication of the datasets, etc.).
 */
public class Service extends EPOSDataModelEntity {

    /**
     * This property refers to a category of the Service. A Service may be associated with multiple categories.
     **/
    private List<String> category;

    /**
     * This property refers to the Contact Point (i.e. Role) defined for the Service.
     */
    private List<LinkedEntity> contactPoint;

    /**
     * This property contains the description of the Service.
     **/
    private String description;

    /**
     * This property contains an identifier for the Service.
     **/
    private String identifier;

    /**
     * This property contains the keywords used to describe the Service. Multiple entries in a keywords list are typically delimited by commas.
     **/
    private String keywords;

    /**
     * This property contains the name of the Service.
     **/
    private String name;

    /**
     * This property refers to a page or document about the Service
     **/
    private String pageURL;

    /**
     * This property refers to an Organisation or Person responsible for making the Service available.
     **/
    private String provider;

    /**
     * This property refers to a geographical location of the Service.
     **/
    private List<Location> spatialExtent;

    /**
     * This property refers to a temporal period (i.e. startDate, endDate) that the Service covers.
     **/
    private List<PeriodOfTime> temporalExtent;

    /**
     * This property refers to the type of the Service.
     **/
    private String type;

    /**
     * This property refers to the Service Contract.
     */
    private LinkedEntity serviceContract;

    public void addSpatialExtent(Location spatialExtent) {
        if (this.getSpatialExtent() == null) {
            ArrayList<Location> spatialExtentList = new ArrayList<>();
            spatialExtentList.add(spatialExtent);
            this.setSpatialExtent(spatialExtentList);
        } else {
            this.getSpatialExtent().add(spatialExtent);
        }
    }

    public void addCategory(String category) {
        if (this.getCategory() == null) {
            ArrayList<String> categoryList = new ArrayList<>();
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

    public void addTemporalExtent(PeriodOfTime temporalExtent) {
        if (this.getTemporalExtent() == null) {
            ArrayList<PeriodOfTime> temporalExtentList = new ArrayList<>();
            temporalExtentList.add(temporalExtent);
            this.setTemporalExtent(temporalExtentList);
        } else {
            this.getTemporalExtent().add(temporalExtent);
        }
    }


    public Service category(List<String> category) {
        this.category = category;
        return this;
    }

    public Service addCategoryItem(String categoryItem) {
        if (this.category == null) {
            this.category = new ArrayList<>();
        }
        this.category.add(categoryItem);
        return this;
    }

    /**
     * This property refers to a category of the Service. A Service may be associated with multiple categories.
     *
     * @return category
     **/

    public List<String> getCategory() {
        return category;
    }

    public void setCategory(List<String> category) {
        this.category = category;
    }

    public Service description(String description) {
        this.description = description;
        return this;
    }

    /**
     * This property contains the description of the Service.
     *
     * @return description
     **/

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Service identifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * This property contains an identifier for the Service.
     *
     * @return identifier
     **/
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Service keywords(String keywords) {
        this.keywords = keywords;
        return this;
    }

    /**
     * This property contains the keywords used to describe the Service. Multiple entries in a keywords list are typically delimited by commas.
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


    public Service name(String name) {
        this.name = name;
        return this;
    }

    /**
     * This property contains the name of the Service.
     *
     * @return name
     **/

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Service pageURL(String pageURL) {
        this.pageURL = pageURL;
        return this;
    }

    /**
     * This property refers to a page or document about the Service
     *
     * @return pageURL
     **/

    public String getPageURL() {
        return pageURL;
    }

    public void setPageURL(String pageURL) {
        this.pageURL = pageURL;
    }

    public Service provider(String provider) {
        this.provider = provider;
        return this;
    }

    /**
     * This property refers to an Organisation or Person responsible for making the Service available.
     *
     * @return provider
     **/

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Service spatialExtent(List<Location> spatialExtent) {
        this.spatialExtent = spatialExtent;
        return this;
    }

    public Service addSpatialExtentItem(Location spatialExtentItem) {
        if (this.spatialExtent == null) {
            this.spatialExtent = new ArrayList<>();
        }
        this.spatialExtent.add(spatialExtentItem);
        return this;
    }

    /**
     * This property refers to a geographical location of the Service.
     *
     * @return spatialExtent
     **/
    public List<Location> getSpatialExtent() {
        return spatialExtent;
    }

    public void setSpatialExtent(ArrayList<Location> spatialExtent) {
        this.spatialExtent = spatialExtent;
    }

    public Service temporalExtent(List<PeriodOfTime> temporalExtent) {
        this.temporalExtent = temporalExtent;
        return this;
    }

    public Service addTemporalExtentItem(PeriodOfTime temporalExtentItem) {
        if (this.temporalExtent == null) {
            this.temporalExtent = new ArrayList<>();
        }
        this.temporalExtent.add(temporalExtentItem);
        return this;
    }

    /**
     * This property refers to a temporal period (i.e. startDate, endDate) that the Service covers.
     *
     * @return temporalExtent
     **/
    public List<PeriodOfTime> getTemporalExtent() {
        return temporalExtent;
    }

    public void setTemporalExtent(List<PeriodOfTime> temporalExtent) {
        this.temporalExtent = temporalExtent;
    }

    public Service type(String type) {
        this.type = type;
        return this;
    }

    /**
     * This property refers to the type of the Service.
     *
     * @return type
     **/

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<LinkedEntity> getContactPoint() {
        return contactPoint;
    }

    public void setContactPoint(List<LinkedEntity> contactPoint) {
        this.contactPoint = contactPoint;
    }


    /**
     * This property refers to the Service Contract.
     *
     * @return serviceContract
     */
    public LinkedEntity getServiceContract() {
        return serviceContract;
    }

    public void setServiceContract(LinkedEntity serviceContract) {
        this.serviceContract = serviceContract;
    }

    @Override
    public String toString() {
        return "Service{" +
                "category=" + category +
                ", contactPoint=" + contactPoint +
                ", description='" + description + '\'' +
                ", identifier='" + identifier + '\'' +
                ", keywords='" + keywords + '\'' +
                ", name='" + name + '\'' +
                ", pageURL='" + pageURL + '\'' +
                ", provider='" + provider + '\'' +
                ", spatialExtent=" + spatialExtent +
                ", temporalExtent=" + temporalExtent +
                ", type='" + type + '\'' +
                ", serviceContract=" + serviceContract +
                '}'+ super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Service service = (Service) o;
        return Objects.equals(getCategory(), service.getCategory()) && Objects.equals(getContactPoint(), service.getContactPoint()) && Objects.equals(getDescription(), service.getDescription()) && Objects.equals(getIdentifier(), service.getIdentifier()) && Objects.equals(getKeywords(), service.getKeywords()) && Objects.equals(getName(), service.getName()) && Objects.equals(getPageURL(), service.getPageURL()) && Objects.equals(getProvider(), service.getProvider()) && Objects.equals(getSpatialExtent(), service.getSpatialExtent()) && Objects.equals(getTemporalExtent(), service.getTemporalExtent()) && Objects.equals(getType(), service.getType()) && Objects.equals(getServiceContract(), service.getServiceContract());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getCategory(), getContactPoint(), getDescription(), getIdentifier(), getKeywords(), getName(), getPageURL(), getProvider(), getSpatialExtent(), getTemporalExtent(), getType(), getServiceContract());
    }
}
