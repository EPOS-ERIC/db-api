package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "payload", schema = "metadata_catalogue")
public class Payload {
    @Id
    @Size(max = 100)
    @Column(name = "instance_id", nullable = false, length = 100)
    private String instanceId;

    @Size(max = 100)
    @Column(name = "meta_id", length = 100)
    private String metaId;

    @Size(max = 1024)
    @Column(name = "uid", length = 1024)
    private String uid;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "version_id")
    private Versioningstatus version;

    @Size(max = 1024)
    @Column(name = "name", length = 1024)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Size(max = 255)
    @Column(name = "content_type")
    private String contentType;

    @Lob
    @Column(name = "schema_definition")
    private String schemaDefinition;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getMetaId() {
        return metaId;
    }

    public void setMetaId(String metaId) {
        this.metaId = metaId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Versioningstatus getVersion() {
        return version;
    }

    public void setVersion(Versioningstatus version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getSchemaDefinition() {
        return schemaDefinition;
    }

    public void setSchemaDefinition(String schemaDefinition) {
        this.schemaDefinition = schemaDefinition;
    }

}