package model;

import jakarta.persistence.*;

@Entity
@Table(name = "facility_address", schema = "metadata_catalogue")
public class FacilityAddress {
    @EmbeddedId
    private FacilityAddressId id;

    @MapsId("facilityInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_instance_id", nullable = false)
    private Facility facilityInstance;

    @MapsId("addressInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_instance_id", nullable = false)
    private Address addressInstance;

    public FacilityAddressId getId() {
        return id;
    }

    public void setId(FacilityAddressId id) {
        this.id = id;
    }

    public Facility getFacilityInstance() {
        return facilityInstance;
    }

    public void setFacilityInstance(Facility facilityInstance) {
        this.facilityInstance = facilityInstance;
    }

    public Address getAddressInstance() {
        return addressInstance;
    }

    public void setAddressInstance(Address addressInstance) {
        this.addressInstance = addressInstance;
    }

}