package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This property contains a name of the category scheme. May be repeated for different versions of the name.
 */
public class CategoryScheme extends EPOSDataModelEntity {

    /**
     * This property contains a description of the category scheme.
     */
	@Schema(name = "description", description = "This property contains a description of the category scheme.", example = "The domain of all seismological assets", required = false)
	private String description;

    /**
     * This property contains a name of the category scheme.
     */
	@Schema(name = "title", description = "This property contains a name of the category scheme.", example = "Seismology", required = false)
	private String title;

	@Schema(name = "code", description = "Code used by Data Portal ", example = "ANT", required = false)
	private String code;

	@Schema(name = "logo", description = "Logo URL used by Data Portal ", example = "assets/img/logo/logo.png", required = false)
	private String logo;

	@Schema(name = "homepage", description = "Homepage URL used by Data Portal ", example = "https://homepage.com", required = false)
	private String homepage;

	@Schema(name = "color", description = "Color used by Data Portal", example = "#06a8e2", required = false)
	private String color;

	@Schema(name = "orderitemnumber", description = "Order number of the categoryscheme on the Data Portal", example = "1", required = false)
    private String orderitemnumber;


    /**
     *  Relates a list of resources of type CATEGORY
     */
	@Schema(name = "topconcepts", description = "Relates a list of resources of type CATEGORY", example = "[{\n" +
			"    \"entityType\": \"CATEGORY\",\n" +
			"    \"instanceId\": \"an UUID\",\n" +
			"    \"metaId\": \"an UUID\",\n" +
			"    \"uid\": \"an UUID\"\n" +
			"  }]", required = false)
	private List<LinkedEntity> topconcepts;


	@Schema(name = "uid", description = "This property contains a preferred UUID of the category", example = "An UUID", required = false)
	private String uid;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
   

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
    
    public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	public String getHomepage() {
		return homepage;
	}

	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getOrderitemnumber() {
		return orderitemnumber;
	}

	public void setOrderitemnumber(String orderitemnumber) {
		this.orderitemnumber = orderitemnumber;
	}

	public List<LinkedEntity> getTopConcepts() {
		return topconcepts;
	}

	public void setTopConcepts(List<LinkedEntity> topconcepts) {
		this.topconcepts = topconcepts;
	}
	
	public void addTopConcepts(LinkedEntity topconcepts) {
        if (this.getTopConcepts() == null) {
            ArrayList<LinkedEntity> broaders = new ArrayList<>();
            broaders.add(topconcepts);
            this.setTopConcepts(broaders);
        } else {
            this.getTopConcepts().add(topconcepts);
        }
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ Objects.hash(code, color, description, homepage, logo, orderitemnumber, title, topconcepts, uid);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CategoryScheme other = (CategoryScheme) obj;
		return Objects.equals(code, other.code) && Objects.equals(color, other.color)
				&& Objects.equals(description, other.description) && Objects.equals(homepage, other.homepage)
				&& Objects.equals(logo, other.logo) && Objects.equals(orderitemnumber, other.orderitemnumber)
				&& Objects.equals(title, other.title) && Objects.equals(topconcepts, other.topconcepts)
				&& Objects.equals(uid, other.uid);
	}

	@Override
	public String toString() {
		return "CategoryScheme{" +
				"description='" + description + '\'' +
				", title='" + title + '\'' +
				", code='" + code + '\'' +
				", logo='" + logo + '\'' +
				", homepage='" + homepage + '\'' +
				", color='" + color + '\'' +
				", orderitemnumber='" + orderitemnumber + '\'' +
				", topconcepts=" + topconcepts +
				", uid='" + uid + '\'' +
				'}'+ super.toString();
	}
}
