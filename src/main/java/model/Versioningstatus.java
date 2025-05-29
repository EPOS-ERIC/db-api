package model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "versioningstatus", schema = "metadata_catalogue")
public class Versioningstatus {
    @Id
    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "version_id", nullable = false, length = 100)
    private String versionId;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "instance_id", length = 100)
    private String instanceId;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "meta_id", length = 100)
    private String metaId;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "uid", length = 1024)
    private String uid;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "instance_change_id", length = 1024)
    private String instanceChangeId;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "provenance", length = 1024)
    private String provenance;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "editor_id", length = 1024)
    private String editorId;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "reviewer_id", length = 100)
    private String reviewerId;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "review_comment", length = 1024)
    private String reviewComment;

    @Lob
    @Column(name = "change_comment")
    private String changeComment;

    @Column(name = "change_timestamp")
    private OffsetDateTime changeTimestamp;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "version", length = 1024)
    private String version;

    @jakarta.validation.constraints.Size(max = 100)
    @Column(name = "status", length = 100)
    private String status;

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

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

    public String getInstanceChangeId() {
        return instanceChangeId;
    }

    public void setInstanceChangeId(String instanceChangeId) {
        this.instanceChangeId = instanceChangeId;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public String getEditorId() {
        return editorId;
    }

    public void setEditorId(String editorId) {
        this.editorId = editorId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getChangeComment() {
        return changeComment;
    }

    public void setChangeComment(String changeComment) {
        this.changeComment = changeComment;
    }

    public OffsetDateTime getChangeTimestamp() {
        return changeTimestamp;
    }

    public void setChangeTimestamp(OffsetDateTime changeTimestamp) {
        this.changeTimestamp = changeTimestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}