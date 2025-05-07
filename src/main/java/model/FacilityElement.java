package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "facility_element")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class FacilityElement {
    @EmbeddedId
    private FacilityElementId id;

    @MapsId("facilityInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_instance_id", nullable = false)
    private Facility facilityInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private Element elementInstance;

    public FacilityElementId getId() {
        return id;
    }

    public void setId(FacilityElementId id) {
        this.id = id;
    }

    public Facility getFacilityInstance() {
        return facilityInstance;
    }

    public void setFacilityInstance(Facility facilityInstance) {
        this.facilityInstance = facilityInstance;
    }

    public Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}