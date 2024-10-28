package model;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment_relation")
public class EquipmentRelation {
    @Id
    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "equipment_instance_id", nullable = false, length = 100)
    private String equipmentInstanceId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "equipment_instance_id", nullable = false)
    private Equipment equipment;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "entity_instance_id", nullable = false, length = 100)
    private String entityInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "resource_entity", nullable = false, length = 100)
    private String resourceEntity;

    public String getEquipmentInstanceId() {
        return equipmentInstanceId;
    }

    public void setEquipmentInstanceId(String equipmentInstanceId) {
        this.equipmentInstanceId = equipmentInstanceId;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public String getEntityInstanceId() {
        return entityInstanceId;
    }

    public void setEntityInstanceId(String entityInstanceId) {
        this.entityInstanceId = entityInstanceId;
    }

    public String getResourceEntity() {
        return resourceEntity;
    }

    public void setResourceEntity(String resourceEntity) {
        this.resourceEntity = resourceEntity;
    }

}