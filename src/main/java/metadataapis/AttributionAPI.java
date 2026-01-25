package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AttributionAPI extends AbstractAPI<org.epos.eposdatamodel.Attribution> {

    private static final Logger LOG = Logger.getLogger(AttributionAPI.class.getName());

    public AttributionAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Attribution obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        boolean roleExplicitlySet = isFieldExplicitlySet(obj, "role");
        boolean agentExplicitlySet = isFieldExplicitlySet(obj, "agent");

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;
        String searchInstanceId = obj.getInstanceId();

        List<Attribution> returnList = getDbaccess().getOneFromDB(searchInstanceId, obj.getMetaId(), obj.getUid(), null, getEdmClass());
        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            Attribution selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Attribution item : returnList) {
                if (item.getVersion() != null && targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }
            oldInstanceId = selectedEntity.getInstanceId();
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
            if (previousObj == null) previousObj = retrieve(selectedEntity.getInstanceId());
        }

        obj = (org.epos.eposdatamodel.Attribution) VersioningStatusAPI.checkVersion(obj, overrideStatus);
        if (obj.getInstanceId() == null) obj.setInstanceId(UUID.randomUUID().toString());
        if (obj.getMetaId() == null) obj.setMetaId(UUID.randomUUID().toString());
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        String newInstanceId = obj.getInstanceId();

        Attribution edmobj = new Attribution();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));

        if (agentExplicitlySet || !isNewVersion) {
            if (obj.getAgent() != null) {
                Organization existingOrg = findOrganizationByLinkedEntity(obj.getAgent(), overrideStatus);
                if (existingOrg != null) {
                    edmobj.setAgentId(existingOrg.getInstanceId());
                    edmobj.setAgentType(obj.getAgent().getEntityType());
                } else {
                    createPendingAgentRelation(edmobj.getInstanceId(), obj.getAgent());
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyAgentFromPreviousVersion(oldInstanceId, edmobj);
        }

        if (roleExplicitlySet || !isNewVersion) {
            if (obj.getRole() != null && !obj.getRole().isEmpty()) {
                RelationSyncUtil.syncSimpleOneToMany(edmobj, edmobj.getInstanceId(), obj.getRole(), model.AttributionRole.class,
                        "attributionInstance", "Role",
                        model.AttributionRole::getRoletype, model.AttributionRole::setRoletype, model.AttributionRole::setAttributionInstance);
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.copySimpleOneToMany(oldInstanceId, edmobj, newInstanceId, model.AttributionRole.class,
                    "attributionInstance", "Role",
                    model.AttributionRole::getRoletype, model.AttributionRole::setRoletype, model.AttributionRole::setAttributionInstance);
        }

        getDbaccess().updateObject(edmobj);
        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.ATTRIBUTION.name(), edmobj);

        return new LinkedEntity().entityType(entityName).instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid());
    }

    private Organization findOrganizationByLinkedEntity(LinkedEntity le, StatusType targetStatus) {
        if (le.getInstanceId() != null) {
            List<Organization> byInstance = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Organization.class);
            if (!byInstance.isEmpty()) return byInstance.get(0);
        }
        if (le.getUid() != null) {
            List<Organization> byUid = EposDataModelDAO.getInstance().getOneFromDBByUID(le.getUid(), Organization.class);
            if (!byUid.isEmpty()) {
                for (Organization org : byUid) {
                    if (org.getVersion() != null && targetStatus != null && targetStatus.toString().equals(org.getVersion().getStatus())) return org;
                }
                return byUid.get(0);
            }
        }
        return null;
    }

    private void createPendingAgentRelation(String attributionInstanceId, LinkedEntity agentLink) {
        if (agentLink == null || agentLink.getUid() == null) return;
        try {
            Versioningstatus pending = new Versioningstatus();
            pending.setVersionId(UUID.randomUUID().toString());
            pending.setInstanceId(UUID.randomUUID().toString());
            pending.setUid(agentLink.getUid());
            pending.setMetaId("ATTRIBUTION_AGENT");
            pending.setStatus(StatusType.PENDING.name());
            pending.setProvenance("ATTRIBUTION");
            pending.setChangeComment(agentLink.getEntityType() != null ? agentLink.getEntityType() + "_AGENT_REF" : "ORGANIZATION_AGENT_REF");
            pending.setReviewComment(attributionInstanceId);
            pending.setChangeTimestamp(java.time.OffsetDateTime.now());
            EposDataModelDAO.getInstance().createObject(pending);
        } catch (Exception e) {
            LOG.warning("Error creating pending agent relation: " + e.getMessage());
        }
    }

    public static void resolvePendingAgentRelations(String organizationUid, String organizationInstanceId) {
        if (organizationUid == null || organizationInstanceId == null) return;
        try {
            List<Versioningstatus> candidates = EposDataModelDAO.getInstance().getOneFromDBBySpecificKeySimpleNoCache("uid", organizationUid, Versioningstatus.class);
            if (candidates == null) return;

            for (Versioningstatus pending : candidates) {
                if (StatusType.PENDING.name().equals(pending.getStatus()) && "ATTRIBUTION_AGENT".equals(pending.getMetaId())) {
                    String attributionInstanceId = pending.getReviewComment();
                    if (attributionInstanceId == null) attributionInstanceId = pending.getInstanceId(); // Fallback

                    List<Attribution> attributionList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(attributionInstanceId, Attribution.class);
                    if (!attributionList.isEmpty()) {
                        Attribution attribution = attributionList.get(0);
                        attribution.setAgentId(organizationInstanceId);
                        String changeComment = pending.getChangeComment();
                        if (changeComment != null && changeComment.endsWith("_AGENT_REF")) {
                            attribution.setAgentType(changeComment.replace("_AGENT_REF", ""));
                        } else {
                            attribution.setAgentType("ORGANIZATION");
                        }
                        EposDataModelDAO.getInstance().updateObject(attribution);
                    }
                    EposDataModelDAO.getInstance().deleteObject(pending);
                }
            }
        } catch (Exception e) {
            LOG.warning("Error resolving pending agent relations: " + e.getMessage());
        }
    }

    // ... resto del file invariato ...
    private void copyAgentFromPreviousVersion(String oldInstanceId, Attribution newEdmobj) {
        List<Attribution> oldList = getDbaccess().getOneFromDBByInstanceId(oldInstanceId, Attribution.class);
        if (oldList != null && !oldList.isEmpty()) {
            Attribution oldAttribution = oldList.get(0);
            if (oldAttribution.getAgentId() != null) {
                newEdmobj.setAgentId(oldAttribution.getAgentId());
                newEdmobj.setAgentType(oldAttribution.getAgentType());
            }
        }
    }
    private boolean isFieldExplicitlySet(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj) != null;
            }
        } catch (Exception e) {}
        return false;
    }
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try { return clazz.getDeclaredField(fieldName); }
            catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }
    @Override public Boolean delete(String instanceId) {
        List<Object> roles = getDbaccess().getJoinEntitiesByParentId("attributionInstance", instanceId, AttributionRole.class);
        if (roles != null) for (Object object : roles) EposDataModelDAO.getInstance().deleteObject(object);
        List<Attribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Attribution.class);
        for (Attribution object : elementList) EposDataModelDAO.getInstance().deleteObject(object);
        return true;
    }
    @Override public org.epos.eposdatamodel.Attribution retrieve(String instanceId) {
        List<Attribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Attribution.class);
        if (elementList == null || elementList.isEmpty()) return null;
        Attribution edmobj = elementList.get(0);
        org.epos.eposdatamodel.Attribution o = new org.epos.eposdatamodel.Attribution();
        o.setInstanceId(edmobj.getInstanceId()); o.setMetaId(edmobj.getMetaId()); o.setUid(edmobj.getUid());
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("attributionInstance", edmobj.getInstanceId(), AttributionRole.class)) {
            AttributionRole item = (AttributionRole) object;
            o.addRole(item.getRoletype());
        }
        if (edmobj.getAgentId() != null && edmobj.getAgentType() != null) {
            o.setAgent(AbstractAPI.retrieveAPI(edmobj.getAgentType()).retrieveLinkedEntity(edmobj.getAgentId()));
        }
        return (org.epos.eposdatamodel.Attribution) VersioningStatusAPI.retrieveVersion(o);
    }
    @Override public org.epos.eposdatamodel.Attribution retrieveByUID(String uid) {
        List<Attribution> returnList = getDbaccess().getOneFromDBByUID(uid, Attribution.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override public List<org.epos.eposdatamodel.Attribution> retrieveBunch(List<String> entities) { return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Attribution.class)); }
    @Override public List<org.epos.eposdatamodel.Attribution> retrieveAll() { return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Attribution.class)); }
    @Override public List<org.epos.eposdatamodel.Attribution> retrieveAllWithStatus(StatusType status) { return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Attribution.class, status)); }
    private List<org.epos.eposdatamodel.Attribution> retrieveEntities(Function<Void, List<String>> dbFetcher) { return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList()); }
    @Override public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Attribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Attribution.class);
        if (elementList != null && !elementList.isEmpty()) {
            Attribution edmobj = elementList.get(0);
            return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.ATTRIBUTION.name());
        }
        return null;
    }
}