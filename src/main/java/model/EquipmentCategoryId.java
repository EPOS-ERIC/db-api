package model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class EquipmentCategoryId implements java.io.Serializable {
    private static final long serialVersionUID = -5028504775258349121L;
    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "equipment_instance_id", nullable = false, length = 100)
    private String equipmentInstanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @jakarta.validation.constraints.NotNull
    @Column(name = "category_instance_id", nullable = false, length = 100)
    private String categoryInstanceId;

    public String getEquipmentInstanceId() {
        return equipmentInstanceId;
    }

    public void setEquipmentInstanceId(String equipmentInstanceId) {
        this.equipmentInstanceId = equipmentInstanceId;
    }

    public String getCategoryInstanceId() {
        return categoryInstanceId;
    }

    public void setCategoryInstanceId(String categoryInstanceId) {
        this.categoryInstanceId = categoryInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.springframework.data.util.ProxyUtils.getUserClass(this) != org.springframework.data.util.ProxyUtils.getUserClass(o))
            return false;
        EquipmentCategoryId entity = (EquipmentCategoryId) o;
        return Objects.equals(this.equipmentInstanceId, entity.equipmentInstanceId) &&
                Objects.equals(this.categoryInstanceId, entity.categoryInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equipmentInstanceId, categoryInstanceId);
    }

}