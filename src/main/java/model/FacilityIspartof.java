package model;

import jakarta.persistence.*;

@Entity
@Table(name = "facility_ispartof")
public class FacilityIspartof {
    @EmbeddedId
    private FacilityIspartofId id;

    @MapsId("facility1InstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "facility1_instance_id", nullable = false)
    private Facility facility1Instance;

    @MapsId("facility2InstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "facility2_instance_id", nullable = false)
    private Facility facility2Instance;

    public FacilityIspartofId getId() {
        return id;
    }

    public void setId(FacilityIspartofId id) {
        this.id = id;
    }

    public Facility getFacility1Instance() {
        return facility1Instance;
    }

    public void setFacility1Instance(Facility facility1Instance) {
        this.facility1Instance = facility1Instance;
    }

    public Facility getFacility2Instance() {
        return facility2Instance;
    }

    public void setFacility2Instance(Facility facility2Instance) {
        this.facility2Instance = facility2Instance;
    }

}