package model;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment_temporal", schema = "metadata_catalogue")
public class EquipmentTemporal {
    @EmbeddedId
    private EquipmentTemporalId id;

    @MapsId("equipmentInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "equipment_instance_id", nullable = false)
    private Equipment equipmentInstance;

    @MapsId("temporalInstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "temporal_instance_id", nullable = false)
    private model.Temporal temporalInstance;

    public EquipmentTemporalId getId() {
        return id;
    }

    public void setId(EquipmentTemporalId id) {
        this.id = id;
    }

    public Equipment getEquipmentInstance() {
        return equipmentInstance;
    }

    public void setEquipmentInstance(Equipment equipmentInstance) {
        this.equipmentInstance = equipmentInstance;
    }

    public model.Temporal getTemporalInstance() {
        return temporalInstance;
    }

    public void setTemporalInstance(model.Temporal temporalInstance) {
        this.temporalInstance = temporalInstance;
    }

}