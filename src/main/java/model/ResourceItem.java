package model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "resource_item")
public class ResourceItem {
    @Id
    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "id", nullable = false, length = 1024)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "processing_unit_id")
    private ProcessingUnit processingUnit;

    @jakarta.validation.constraints.NotNull
    @Column(name = "addition_time", nullable = false)
    private OffsetDateTime additionTime;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "resource_uid", nullable = false, length = 1024)
    private String resourceUid;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "resource_url", length = 1024)
    private String resourceUrl;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "resource_version", length = 1024)
    private String resourceVersion;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "file_name", length = 1024)
    private String fileName;

    @Lob
    @Column(name = "file_description")
    private String fileDescription;

    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "file_type", length = 1024)
    private String fileType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ProcessingUnit getProcessingUnit() {
        return processingUnit;
    }

    public void setProcessingUnit(ProcessingUnit processingUnit) {
        this.processingUnit = processingUnit;
    }

    public OffsetDateTime getAdditionTime() {
        return additionTime;
    }

    public void setAdditionTime(OffsetDateTime additionTime) {
        this.additionTime = additionTime;
    }

    public String getResourceUid() {
        return resourceUid;
    }

    public void setResourceUid(String resourceUid) {
        this.resourceUid = resourceUid;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileDescription() {
        return fileDescription;
    }

    public void setFileDescription(String fileDescription) {
        this.fileDescription = fileDescription;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

/*
 TODO [Reverse Engineering] create field to map the 'status' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "status", columnDefinition = "processing_item_status not null")
    private Object status;
*/
}