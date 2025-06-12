package org.epos.eposdatamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A device.
 */
public class Equipment extends EPOSDataModelEntity {

	/**
	 * This property refers to a category of the Equipment. An Equipment may be associated with multiple categories.
	 **/
	private List<LinkedEntity> category;

	/**
	 * This property contains contact information (i.e. Role) that can be used for sending comments about the Equipment.
	 */
	private List<LinkedEntity> contactPoint;

	/**
	 * This property contains a free-text description of the Equipment.
	 **/
	private String description;

	/**
	 * This property contains the full-scale measuring ability, in nT (unit and value).
	 **/
	private String dynamicRange;

	/**
	 * This property describes the filter that the instrument uses to produce data.
	 **/
	private String filter;

	/**
	 * This property refers to an Equipment or a Facility in which the described Equipment is included.
	 */
	private List<LinkedEntity> isPartOf;

	/**
	 * This property refers to an ORGANIZATION responsible for manufacturing the Equipment.
	 */
	private LinkedEntity manufacturer;

	/**
	 * This property contains a name given to the Equipment.
	 **/
	private String name;
	
	/**
	 * This property contains a name given to the Equipment.
	 **/
	private String identifier;

	/**
	 * This property refers to a page or document about the Equipment
	 **/
	private String pageURL;

	/**
	 * This property describes how the instrument is oriented.
	 **/
	private String orientation;

	/**
	 * It represents the link to another Epos resource. e.g., Dataset, WebService, Service
	 */
	private List<LinkedEntity> relation;

	/**
	 * This property contains the resolution in nT.
	 **/
	private String resolution;

	/**
	 * This property contains the sample period in mS (unit and value)
	 **/
	private String samplePeriod;

	/**
	 * This property contains the serial number for the Equipment.
	 **/
	private String serialNumber;

	/**
	 * This property refers to a geographical location of the Equipment. LinkedEntity of type LOCATION
	 **/
	private List<LinkedEntity> spatialExtent;

	/**
	 * This property refers to a temporal period (i.e. startDate, endDate) that the Equipment covers. LinkedEntity of type PERIODOFTIME
	 **/
	private List<LinkedEntity> temporalExtent;

	private String keywords;

	/**
	 * This property refers to the type of the Equipment.
	 **/
	private String type;

	public void addIsPartOf(LinkedEntity isPartOf) {
		if (this.getIsPartOf() == null) {
			ArrayList<LinkedEntity> isPartOfList = new ArrayList<>();
			isPartOfList.add(isPartOf);
			this.setIsPartOf(isPartOfList);
		} else {
			this.getIsPartOf().add(isPartOf);
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

	public void addSpatialExtent(LinkedEntity spatialExtent) {
		if (this.getSpatialExtent() == null) {
			ArrayList<LinkedEntity> spatialExtentList = new ArrayList<>();
			spatialExtentList.add(spatialExtent);
			this.setSpatialExtent(spatialExtentList);
		} else {
			this.getSpatialExtent().add(spatialExtent);
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

	public void addCategory(LinkedEntity category) {
		if (this.getCategory() == null) {
			ArrayList<LinkedEntity> categoryList = new ArrayList<>();
			categoryList.add(category);
			this.setCategory(categoryList);
		} else {
			this.getCategory().add(category);
		}
	}

	public Equipment category(List<LinkedEntity> category) {
		this.category = category;
		return this;
	}

	public Equipment addCategoryItem(LinkedEntity categoryItem) {
		if (this.category == null) {
			this.category = new ArrayList<>();
		}
		this.category.add(categoryItem);
		return this;
	}

	/**
	 * This property refers to a category of the Equipment. An Equipment may be associated with multiple categories.
	 *
	 * @return category
	 **/

	public List<LinkedEntity> getCategory() {
		return category;
	}

	public void setCategory(List<LinkedEntity> category) {
		this.category = category;
	}


	public Equipment description(String description) {
		this.description = description;
		return this;
	}

	/**
	 * This property contains a free-text description of the Equipment.
	 *
	 * @return description
	 **/

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Equipment dynamicRange(String dynamicRange) {
		this.dynamicRange = dynamicRange;
		return this;
	}

	/**
	 * This property contains the full-scale measuring ability, in nT (unit and value).
	 *
	 * @return dynamicRange
	 **/

	public String getDynamicRange() {
		return dynamicRange;
	}

	public void setDynamicRange(String dynamicRange) {
		this.dynamicRange = dynamicRange;
	}

	public Equipment filter(String filter) {
		this.filter = filter;
		return this;
	}

	/**
	 * This property describes the filter that the instrument uses to produce data.
	 *
	 * @return filter
	 **/

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}


	public Equipment identifier(String identifier) {
		this.identifier = identifier;
		return this;
	}

	/**
	 * This property contains a identifier given to the Equipment.
	 *
	 * @return identifier
	 **/

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public Equipment name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * This property contains a name given to the Equipment.
	 *
	 * @return name
	 **/

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Equipment pageURL(String pageURL) {
		this.pageURL = pageURL;
		return this;
	}

	/**
	 * This property refers to a page or document about the Equipment
	 *
	 * @return pageURL
	 **/

	public String getPageURL() {
		return pageURL;
	}

	public void setPageURL(String pageURL) {
		this.pageURL = pageURL;
	}

	public Equipment orientation(String orientation) {
		this.orientation = orientation;
		return this;
	}

	/**
	 * This property describes how the instrument is oriented.
	 *
	 * @return orientation
	 **/

	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}


	public Equipment resolution(String resolution) {
		this.resolution = resolution;
		return this;
	}

	/**
	 * This property contains the resolution in nT.
	 *
	 * @return resolution
	 **/

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public Equipment samplePeriod(String samplePeriod) {
		this.samplePeriod = samplePeriod;
		return this;
	}

	/**
	 * This property contains the sample period in mS (unit and value)
	 *
	 * @return samplePeriod
	 **/

	public String getSamplePeriod() {
		return samplePeriod;
	}

	public void setSamplePeriod(String samplePeriod) {
		this.samplePeriod = samplePeriod;
	}

	public Equipment serialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
		return this;
	}

	/**
	 * This property contains the serial number for the Equipment.
	 *
	 * @return serialNumber
	 **/

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public Equipment spatialExtent(List<LinkedEntity> spatialExtent) {
		this.spatialExtent = spatialExtent;
		return this;
	}

	public Equipment addSpatialExtentItem(LinkedEntity spatialExtentItem) {
		if (this.spatialExtent == null) {
			this.spatialExtent = new ArrayList<>();
		}
		this.spatialExtent.add(spatialExtentItem);
		return this;
	}

	/**
	 * This property refers to a geographical location of the Equipment.
	 *
	 * @return spatialExtent
	 **/
	public List<LinkedEntity> getSpatialExtent() {
		return spatialExtent;
	}

	public void setSpatialExtent(ArrayList<LinkedEntity> spatialExtent) {
		this.spatialExtent = spatialExtent;
	}

	public Equipment temporalExtent(List<LinkedEntity> temporalExtent) {
		this.temporalExtent = temporalExtent;
		return this;
	}

	public Equipment addTemporalExtentItem(LinkedEntity temporalExtentItem) {
		if (this.temporalExtent == null) {
			this.temporalExtent = new ArrayList<>();
		}
		this.temporalExtent.add(temporalExtentItem);
		return this;
	}

	/**
	 * This property refers to a temporal period (i.e. startDate, endDate) that the Equipment covers.
	 *
	 * @return temporalExtent
	 **/
	public List<LinkedEntity> getTemporalExtent() {
		return temporalExtent;
	}

	public void setTemporalExtent(List<LinkedEntity> temporalExtent) {
		this.temporalExtent = temporalExtent;
	}

	public Equipment type(String type) {
		this.type = type;
		return this;
	}

	/**
	 * This property refers to the type of the Equipment.
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

	public List<LinkedEntity> getIsPartOf() {
		return isPartOf;
	}

	public void setIsPartOf(List<LinkedEntity> isPartOf) {
		this.isPartOf = isPartOf;
	}

	public LinkedEntity getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(LinkedEntity manufacturer) {
		this.manufacturer = manufacturer;
	}

	public List<LinkedEntity> getRelation() {
		return relation;
	}

	public void setRelation(List<LinkedEntity> relation) {
		this.relation = relation;

	}

	public Equipment keywords(String keywords) {
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

	@Override
	public String toString() {
		return "Equipment{" +
				"category=" + category +
				", contactPoint=" + contactPoint +
				", description='" + description + '\'' +
				", dynamicRange='" + dynamicRange + '\'' +
				", filter='" + filter + '\'' +
				", isPartOf=" + isPartOf +
				", manufacturer=" + manufacturer +
				", name='" + name + '\'' +
				", identifier='" + identifier + '\'' +
				", pageURL='" + pageURL + '\'' +
				", orientation='" + orientation + '\'' +
				", relation=" + relation +
				", resolution='" + resolution + '\'' +
				", samplePeriod='" + samplePeriod + '\'' +
				", serialNumber='" + serialNumber + '\'' +
				", spatialExtent=" + spatialExtent +
				", temporalExtent=" + temporalExtent +
				", keywords='" + keywords + '\'' +
				", type='" + type + '\'' +
				'}'+ super.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Equipment equipment = (Equipment) o;
		return Objects.equals(getUid(), equipment.getUid()) && Objects.equals(getCategory(), equipment.getCategory()) && Objects.equals(getContactPoint(), equipment.getContactPoint()) && Objects.equals(getDescription(), equipment.getDescription()) && Objects.equals(getDynamicRange(), equipment.getDynamicRange()) && Objects.equals(getFilter(), equipment.getFilter()) && Objects.equals(getIsPartOf(), equipment.getIsPartOf()) && Objects.equals(getManufacturer(), equipment.getManufacturer()) && Objects.equals(getName(), equipment.getName()) && Objects.equals(getPageURL(), equipment.getPageURL()) && Objects.equals(getOrientation(), equipment.getOrientation()) && Objects.equals(getRelation(), equipment.getRelation()) && Objects.equals(getResolution(), equipment.getResolution()) && Objects.equals(getSamplePeriod(), equipment.getSamplePeriod()) && Objects.equals(getSerialNumber(), equipment.getSerialNumber()) && Objects.equals(getSpatialExtent(), equipment.getSpatialExtent()) && Objects.equals(getTemporalExtent(), equipment.getTemporalExtent()) && Objects.equals(getType(), equipment.getType());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getCategory(), getContactPoint(), getDescription(), getDynamicRange(), getFilter(), getIsPartOf(), getManufacturer(), getName(), getPageURL(), getOrientation(), getRelation(), getResolution(), getSamplePeriod(), getSerialNumber(), getSpatialExtent(), getTemporalExtent(), getType());
	}
}
