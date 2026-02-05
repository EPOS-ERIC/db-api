package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "edm_entity_id", schema = "metadata_catalogue")
public class EdmEntityId {
    @Id
    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "meta_id", nullable = false, length = 100)
    private String metaId;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "table_name", length = 1024)
    private String tableName;

    public String getMetaId() {
        return metaId;
    }

    public void setMetaId(String metaId) {
        this.metaId = metaId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

}