package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plugin_relations")
public class PluginRelation {
    @Id
    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "id", nullable = false, length = 1024)
    private String id;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "plugin_id", nullable = false, length = 1024)
    private String pluginId;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "relation_id", nullable = false, length = 1024)
    private String relationId;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "relation_type", nullable = false, length = 1024)
    private String relationType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getRelationId() {
        return relationId;
    }

    public void setRelationId(String relationId) {
        this.relationId = relationId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

}