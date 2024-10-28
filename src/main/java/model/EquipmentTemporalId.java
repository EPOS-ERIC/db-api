package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class EquipmentTemporalId implements java.io.Serializable {
    private static final long serialVersionUID = 336486098540499773L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "equipment_instance_id", nullable = false, length = 100)
    private String equipmentInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "temporal_instance_id", nullable = false, length = 100)
    private String temporalInstanceId;

    public String getEquipmentInstanceId() {
        return equipmentInstanceId;
    }

    public void setEquipmentInstanceId(String equipmentInstanceId) {
        this.equipmentInstanceId = equipmentInstanceId;
    }

    public String getTemporalInstanceId() {
        return temporalInstanceId;
    }

    public void setTemporalInstanceId(String temporalInstanceId) {
        this.temporalInstanceId = temporalInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        EquipmentTemporalId entity = (EquipmentTemporalId) o;
        return Objects.equals(this.equipmentInstanceId, entity.equipmentInstanceId) &&
                Objects.equals(this.temporalInstanceId, entity.temporalInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equipmentInstanceId, temporalInstanceId);
    }

}