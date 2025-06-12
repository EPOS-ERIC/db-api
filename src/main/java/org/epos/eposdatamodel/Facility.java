package org.epos.eposdatamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This refers to facilities with resources and related services used by the scientific community to conduct top-level
 * research in their respective fields, ranging from social sciences to astronomy, genomics to nanotechnologies.
 * Examples include laboratories, special habitats, libraries, databases, biological archives, etc. A type of facility could be
 * a research infrastructure a higher aggregated entity.
 */
public class Facility extends EPOSDataModelEntity {

	/**
	 * This property contains the physical address of the Facility.
	 */
	private List<LinkedEntity> address;

	/**
	 * This property refers to a category of the Facility. A Facility may be associated with multiple categories.
	 **/
	private List<LinkedEntity> category;

	/**
	 * This property contains contact information (i.e. Role) that can be used for sending comments about the Facility
	 */
	private List<LinkedEntity> contactPoint;

	/**
	 * This property contains a free-text description of the Facility.
	 **/
	private String description;
	
	/**
	 * This property contains a free-text description of the Facility.
	 **/
	private String identifier;

	/**
	 * This property refers to a Facility in which the described Facility is included.
	 */
	private List<LinkedEntity> isPartOf;

	/**
	 * This property refers to a page or document about this Facility.
	 **/
	private List<String> pageURL;

	/**
	 * It represents the link to another Epos resource. e.g., Dataset, WebService, Service
	 */
	private List<LinkedEntity> relation;

	/**
	 * This property refers to a geographical location of the Facility.
	 **/
	private List<LinkedEntity> spatialExtent;

	/**
	 * This property contains a name given to the Facility.
	 **/
	private String title;
	

	public Facility identifier(String identifier) {
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
	

	/**
	 * This property refers to the type of the Facility.
	 **/
	private String type;


	private String keywords;


	public Facility keywords(String keywords) {
		this.keywords = keywords;
		return this;
	}

	public void setKeywords(String keyword) {
		this.keywords = keyword;
	}

	/**
	 * This property contains a keyword or tag describing the Data Product. Multiple entries in a keywords list are typically delimited by commas.
	 *
	 * @return keywords
	 **/

	public String getKeywords() {
		return keywords;
	}



	public void addRelation(LinkedEntity relation) {
		if (this.getRelation() == null) {
			ArrayList<LinkedEntity> relationList = new ArrayList<>();
			relationList.add(relation);
			this.setRelation(relationList);
		} else {
			this.getRelation().add(relation);
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

	public void addPageURL(String pageURL) {
		if (this.getPageURL() == null) {
			ArrayList<String> pageURLList = new ArrayList<>();
			pageURLList.add(pageURL);
			this.setPageURL(pageURLList);
		} else {
			this.getPageURL().add(pageURL);
		}
	}

	public Facility address(List<LinkedEntity> address) {
		this.address = address;
		return this;
	}

	/**
	 * Get address
	 *
	 * @return address
	 **/

	public List<LinkedEntity> getAddress() {
		return address;
	}

	public void setAddress(List<LinkedEntity> address) {
		this.address = address;
	}

	public Facility addAddress(LinkedEntity address) {
		if (this.address == null) {
			this.address = new ArrayList<>();
		}
		this.address.add(address);
		return this;
	}

	public Facility category(List<LinkedEntity> category) {
		this.category = category;
		return this;
	}

	public Facility addCategoryItem(LinkedEntity categoryItem) {
		if (this.category == null) {
			this.category = new ArrayList<>();
		}
		this.category.add(categoryItem);
		return this;
	}

	/**
	 * This property refers to a category of the Facility. A Facility may be associated with multiple categories.
	 *
	 * @return category
	 **/

	public List<LinkedEntity> getCategory() {
		return category;
	}

	public void setCategory(List<LinkedEntity> category) {
		this.category = category;
	}


	public Facility description(String description) {
		this.description = description;
		return this;
	}

	/**
	 * This property contains a free-text description of the Facility.
	 *
	 * @return description
	 **/

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


	public Facility pageURL(List<String> pageURL) {
		this.pageURL = pageURL;
		return this;
	}

	public Facility addPageURLItem(String pageURLItem) {
		if (this.pageURL == null) {
			this.pageURL = new ArrayList<>();
		}
		this.pageURL.add(pageURLItem);
		return this;
	}

	/**
	 * This property refers to a page or document about this Facility.
	 *
	 * @return pageURL
	 **/

	public List<String> getPageURL() {
		return pageURL;
	}

	public void setPageURL(List<String> pageURL) {
		this.pageURL = pageURL;
	}


	public Facility spatialExtent(List<LinkedEntity> spatialExtent) {
		this.spatialExtent = spatialExtent;
		return this;
	}

	public Facility addSpatialExtentItem(LinkedEntity spatialExtentItem) {
		if (this.spatialExtent == null) {
			this.spatialExtent = new ArrayList<>();
		}
		this.spatialExtent.add(spatialExtentItem);
		return this;
	}

	/**
	 * This property refers to a geographical location of the Facility.
	 *
	 * @return spatialExtent
	 **/
	public List<LinkedEntity> getSpatialExtent() {
		return spatialExtent;
	}

	public void setSpatialExtent(ArrayList<LinkedEntity> spatialExtent) {
		this.spatialExtent = spatialExtent;
	}

	public Facility title(String title) {
		this.title = title;
		return this;
	}

	/**
	 * This property contains a name given to the Facility.
	 *
	 * @return title
	 **/

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Facility type(String type) {
		this.type = type;
		return this;
	}

	/**
	 * This property refers to the type of the Facility.
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

	public List<LinkedEntity> getRelation() {
		return relation;
	}

	public void setRelation(List<LinkedEntity> relation) {
		this.relation = relation;
	}

	@Override
	public String toString() {
		return "Facility{" +
				"address=" + address +
				", category=" + category +
				", contactPoint=" + contactPoint +
				", description='" + description + '\'' +
				", identifier='" + identifier + '\'' +
				", isPartOf=" + isPartOf +
				", pageURL=" + pageURL +
				", relation=" + relation +
				", spatialExtent=" + spatialExtent +
				", title='" + title + '\'' +
				", type='" + type + '\'' +
				", keywords='" + keywords + '\'' +
				'}'+ super.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Facility facility = (Facility) o;
		return Objects.equals(getUid(), facility.getUid()) && Objects.equals(getAddress(), facility.getAddress()) && Objects.equals(getCategory(), facility.getCategory()) && Objects.equals(getContactPoint(), facility.getContactPoint()) && Objects.equals(getDescription(), facility.getDescription()) && Objects.equals(getIsPartOf(), facility.getIsPartOf()) && Objects.equals(getPageURL(), facility.getPageURL()) && Objects.equals(getRelation(), facility.getRelation()) && Objects.equals(getSpatialExtent(), facility.getSpatialExtent()) && Objects.equals(getTitle(), facility.getTitle()) && Objects.equals(getType(), facility.getType());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getAddress(), getCategory(), getContactPoint(), getDescription(), getIsPartOf(), getPageURL(), getRelation(), getSpatialExtent(), getTitle(), getType());
	}

}
