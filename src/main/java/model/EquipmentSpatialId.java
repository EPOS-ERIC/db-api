package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class EquipmentSpatialId implements java.io.Serializable {
    private static final long serialVersionUID = -240850242890053943L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "equipment_instance_id", nullable = false, length = 100)
    private String equipmentInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "spatial_instance_id", nullable = false, length = 100)
    private String spatialInstanceId;

    public String getEquipmentInstanceId() {
        return equipmentInstanceId;
    }

    public void setEquipmentInstanceId(String equipmentInstanceId) {
        this.equipmentInstanceId = equipmentInstanceId;
    }

    public String getSpatialInstanceId() {
        return spatialInstanceId;
    }

    public void setSpatialInstanceId(String spatialInstanceId) {
        this.spatialInstanceId = spatialInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        EquipmentSpatialId entity = (EquipmentSpatialId) o;
        return Objects.equals(this.equipmentInstanceId, entity.equipmentInstanceId) &&
                Objects.equals(this.spatialInstanceId, entity.spatialInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equipmentInstanceId, spatialInstanceId);
    }

}