package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class EquipmentContactpointId implements java.io.Serializable {
    private static final long serialVersionUID = -8209650162171320136L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "equipment_instance_id", nullable = false, length = 100)
    private String equipmentInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "contactpoint_instance_id", nullable = false, length = 100)
    private String contactpointInstanceId;

    public String getEquipmentInstanceId() {
        return equipmentInstanceId;
    }

    public void setEquipmentInstanceId(String equipmentInstanceId) {
        this.equipmentInstanceId = equipmentInstanceId;
    }

    public String getContactpointInstanceId() {
        return contactpointInstanceId;
    }

    public void setContactpointInstanceId(String contactpointInstanceId) {
        this.contactpointInstanceId = contactpointInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        EquipmentContactpointId entity = (EquipmentContactpointId) o;
        return Objects.equals(this.equipmentInstanceId, entity.equipmentInstanceId) &&
                Objects.equals(this.contactpointInstanceId, entity.contactpointInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equipmentInstanceId, contactpointInstanceId);
    }

}