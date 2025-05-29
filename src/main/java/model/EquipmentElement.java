package model;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment_element", schema = "metadata_catalogue")
public class EquipmentElement {
    @EmbeddedId
    private EquipmentElementId id;

    @MapsId("equipmentInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_instance_id", nullable = false)
    private Equipment equipmentInstance;

    @MapsId("elementInstanceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "element_instance_id", nullable = false)
    private Element elementInstance;

    public EquipmentElementId getId() {
        return id;
    }

    public void setId(EquipmentElementId id) {
        this.id = id;
    }

    public Equipment getEquipmentInstance() {
        return equipmentInstance;
    }

    public void setEquipmentInstance(Equipment equipmentInstance) {
        this.equipmentInstance = equipmentInstance;
    }

    public Element getElementInstance() {
        return elementInstance;
    }

    public void setElementInstance(Element elementInstance) {
        this.elementInstance = elementInstance;
    }

}