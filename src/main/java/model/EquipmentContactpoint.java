package model;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment_contactpoint")
public class EquipmentContactpoint {
    @EmbeddedId
    private EquipmentContactpointId id;

    @MapsId("equipmentInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "equipment_instance_id", nullable = false)
    private Equipment equipmentInstance;

    @MapsId("contactpointInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "contactpoint_instance_id", nullable = false)
    private Contactpoint contactpointInstance;

    public EquipmentContactpointId getId() {
        return id;
    }

    public void setId(EquipmentContactpointId id) {
        this.id = id;
    }

    public Equipment getEquipmentInstance() {
        return equipmentInstance;
    }

    public void setEquipmentInstance(Equipment equipmentInstance) {
        this.equipmentInstance = equipmentInstance;
    }

    public Contactpoint getContactpointInstance() {
        return contactpointInstance;
    }

    public void setContactpointInstance(Contactpoint contactpointInstance) {
        this.contactpointInstance = contactpointInstance;
    }

}