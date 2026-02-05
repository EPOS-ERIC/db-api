package model;

import jakarta.persistence.*;

@Entity
@Table(name = "category_ispartof", schema = "metadata_catalogue")
public class CategoryIspartof {
    @EmbeddedId
    private CategoryIspartofId id;

    @MapsId("category1InstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category1_instance_id", nullable = false)
    private Category category1Instance;

    @MapsId("category2InstanceId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category2_instance_id", nullable = false)
    private Category category2Instance;

    public CategoryIspartofId getId() {
        return id;
    }

    public void setId(CategoryIspartofId id) {
        this.id = id;
    }

    public Category getCategory1Instance() {
        return category1Instance;
    }

    public void setCategory1Instance(Category category1Instance) {
        this.category1Instance = category1Instance;
    }

    public Category getCategory2Instance() {
        return category2Instance;
    }

    public void setCategory2Instance(Category category2Instance) {
        this.category2Instance = category2Instance;
    }

}