package model;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment_spatial", schema = "metadata_catalogue")
public class EquipmentSpatial {
    @EmbeddedId
    private EquipmentSpatialId id;

    @MapsId("equipmentInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_instance_id", nullable = false)
    private Equipment equipmentInstance;

    @MapsId("spatialInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spatial_instance_id", nullable = false)
    private model.Spatial spatialInstance;

    public EquipmentSpatialId getId() {
        return id;
    }

    public void setId(EquipmentSpatialId id) {
        this.id = id;
    }

    public Equipment getEquipmentInstance() {
        return equipmentInstance;
    }

    public void setEquipmentInstance(Equipment equipmentInstance) {
        this.equipmentInstance = equipmentInstance;
    }

    public model.Spatial getSpatialInstance() {
        return spatialInstance;
    }

    public void setSpatialInstance(model.Spatial spatialInstance) {
        this.spatialInstance = spatialInstance;
    }

}