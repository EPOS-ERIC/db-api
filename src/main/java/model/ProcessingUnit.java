package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

import java.time.OffsetDateTime;

@Entity
@Table(name = "processing_unit")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class ProcessingUnit {
    @Id
    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "id", nullable = false, length = 1024)
    private String id;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "name", nullable = false, length = 1024)
    private String name;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "environment_unit_url", length = 1024)
    private String environmentUnitUrl;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "environment_unit_id", length = 1024)
    private String environmentUnitId;

    @Lob
    @Column(name = "description")
    private String description;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "icsd_user_id", nullable = false, length = 1024)
    private String icsdUserId;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "processing_environment_type_id", nullable = false, length = 1024)
    private String processingEnvironmentTypeId;

    @jakarta.validation.constraints.NotNull
    @Column(name = "creation_time", nullable = false)
    private OffsetDateTime creationTime;

    @jakarta.validation.constraints.NotNull
    @Column(name = "changed_time", nullable = false)
    private OffsetDateTime changedTime;

    @jakarta.validation.constraints.Size(max = 10485760)
    @Column(name = "additional_information", length = 10485760)
    private String additionalInformation;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvironmentUnitUrl() {
        return environmentUnitUrl;
    }

    public void setEnvironmentUnitUrl(String environmentUnitUrl) {
        this.environmentUnitUrl = environmentUnitUrl;
    }

    public String getEnvironmentUnitId() {
        return environmentUnitId;
    }

    public void setEnvironmentUnitId(String environmentUnitId) {
        this.environmentUnitId = environmentUnitId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcsdUserId() {
        return icsdUserId;
    }

    public void setIcsdUserId(String icsdUserId) {
        this.icsdUserId = icsdUserId;
    }

    public String getProcessingEnvironmentTypeId() {
        return processingEnvironmentTypeId;
    }

    public void setProcessingEnvironmentTypeId(String processingEnvironmentTypeId) {
        this.processingEnvironmentTypeId = processingEnvironmentTypeId;
    }

    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(OffsetDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public OffsetDateTime getChangedTime() {
        return changedTime;
    }

    public void setChangedTime(OffsetDateTime changedTime) {
        this.changedTime = changedTime;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

/*
 TODO [Reverse Engineering] create field to map the 'status' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "status", columnDefinition = "processing_unit_status not null")
    private Object status;
*/
}