package org.epos.eposdatamodel;

import io.swagger.v3.oas.annotations.media.Schema;
import metadataapis.EntityNames;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;


/**
 * Object to link different entity to each other and be transparent to the versioning and approval process.
 */
public class LinkedEntity {

    /**
     * The instanceId of the related instance, it can be used to precisely refer to the ote instance.
     */
    @Schema(name = "instanceId", description = "The instanceId of the related instance, it can be used to precisely refer to the ote instance", example = "12414324252352", required = false)
    private String instanceId;

    /**
     * The uid of the related instance.
     */
    @Schema(name = "uid", description = "The uid of the related instance.", example = "12414324252352", required = false)
    private String uid;

    /**
     * The entity type of the related instance (e.g. DataProduct, Equipment...)
     */
    @Schema(name = "entityType", description = "The entity type of the related instance in upper case (e.g. DataProduct, Equipment...)", example = "ORGANIZATION", required = false)
    private String entityType;

    /**
     * The metaId of the related instance
     */
    @Schema(name = "metaId", description = "The metaId of the related instance", example = "12414324252352", required = false)
    private String metaId;


    public LinkedEntity instanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    public LinkedEntity uid(String uid) {
        this.uid = uid;
        return this;
    }

    public LinkedEntity entityType(String entityType) {
    	this.entityType = entityType;
        return this;
    }

    public LinkedEntity metaId(String metaId) {
        this.metaId = metaId;
        return this;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getMetaId() {
        return metaId;
    }

    public void setMetaId(String metaId) {
        this.metaId = metaId;
    }

    public static Boolean contains(List<LinkedEntity> list, LinkedEntity linkedEntity){
        for(LinkedEntity linkedEntity1 : list){
            System.out.println(linkedEntity1.toString());
            System.out.println(linkedEntity.toString());
            if(linkedEntity1.equals(linkedEntity)){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkedEntity that = (LinkedEntity) o;
        return Objects.equals(instanceId, that.instanceId) && Objects.equals(uid, that.uid) && Objects.equals(entityType, that.entityType) && Objects.equals(metaId, that.metaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, uid, entityType, metaId);
    }

    @Override
    public String toString() {
        return "LinkedEntity{" +
                "instanceId='" + instanceId + '\'' +
                ", uid='" + uid + '\'' +
                ", entityType='" + entityType + '\'' +
                ", metaId='" + metaId + '\'' +
                '}';
    }
}
