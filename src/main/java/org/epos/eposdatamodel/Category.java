package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A subject of a Class.
 */
public class Category extends EPOSDataModelEntity {
    /**
     * This property contains a description of the category
     */
    @Schema(name = "description", description = "This property contains a description of the category", example = "Refer to seismological events", required = false)
    private String description;

    /**
     * Relates a resource of type CATEGORYSCHEME
     */
    @Schema(name = "inScheme", description = "Relates a resource of type CATEGORYSCHEME", example = "{\n" +
            "    \"entityType\": \"CATEGORYSCHEME\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }", required = false)
    private LinkedEntity inScheme;

    /**
     * This property contains a preferred label of the category
     */
    @Schema(name = "name", description = "This property contains a preferred label of the category", example = "Seismological events", required = false)
    private String name;
    
    /**
     * This property contains a UUID of the category .
     */
    @Schema(name = "uid", description = "This property contains a preferred UUID of the category", example = "An UUID", required = false)
    private String uid;
    
    /**
     * Relates a list of resources of type CATEGORY
     */
    @Schema(name = "broader", description = "Relates a list of resources CATEGORY", example = "[{\n" +
            "    \"entityType\": \"CATEGORY\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> broader;
    
    /**
     * Relates a list of resources of type CATEGORY
     */
    @Schema(name = "narrower", description = "Relates a list of resources CATEGORY", example = "[{\n" +
            "    \"entityType\": \"CATEGORY\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> narrower;

    /**
     * The CATEGORY used as top concept
     */
    @Schema(name = "topConcept", description = "The CATEGORY used as top concept", example = "[{\n" +
            "    \"entityType\": \"CATEGORY\",\n" +
            "    \"instanceId\": \"an UUID\",\n" +
            "    \"metaId\": \"an UUID\",\n" +
            "    \"uid\": \"an UUID\"\n" +
            "  }]", required = false)
    private List<LinkedEntity> topConcept;


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LinkedEntity getInScheme() {
        return inScheme;
    }

    public void setInScheme(LinkedEntity inScheme) {
        this.inScheme = inScheme;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<LinkedEntity> getBroader() {
		return broader;
	}

	public void setBroader(List<LinkedEntity> broader) {
		this.broader = broader;
	}

	public List<LinkedEntity> getNarrower() {
		return narrower;
	}

	public void setNarrower(List<LinkedEntity> narrower) {
		this.narrower = narrower;
	}
	
	public void addNarrower(LinkedEntity narrower) {
        if (this.getNarrower() == null) {
            ArrayList<LinkedEntity> narrowers = new ArrayList<>();
            narrowers.add(narrower);
            this.setNarrower(narrowers);
        } else {
            this.getNarrower().add(narrower);
        }
    }

	public void addBroader(LinkedEntity broader) {
        if (this.getBroader() == null) {
            ArrayList<LinkedEntity> broaders = new ArrayList<>();
            broaders.add(broader);
            this.setBroader(broaders);
        } else {
            this.getBroader().add(broader);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Category category = (Category) o;
        return Objects.equals(description, category.description) && Objects.equals(inScheme, category.inScheme) && Objects.equals(name, category.name) && Objects.equals(uid, category.uid) && Objects.equals(broader, category.broader) && Objects.equals(narrower, category.narrower) && Objects.equals(topConcept, category.topConcept);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), description, inScheme, name, uid, broader, narrower, topConcept);
    }

    @Override
    public String toString() {
        return "Category{" +
                "description='" + description + '\'' +
                ", inScheme=" + inScheme +
                ", name='" + name + '\'' +
                ", uid='" + uid + '\'' +
                ", broader=" + broader +
                ", narrower=" + narrower +
                ", topConcept=" + topConcept +
                '}';
    }
}