-- Apply through the database migration process. Review with EXPLAIN (ANALYZE,
-- BUFFERS) on production-sized data before retaining every index.
-- Do not use CREATE INDEX CONCURRENTLY here: migration runners commonly wrap
-- scripts in a transaction.

CREATE INDEX IF NOT EXISTS idx_versioningstatus_instance_status
    ON metadata_catalogue.versioningstatus (instance_id, status);
CREATE INDEX IF NOT EXISTS idx_versioningstatus_uid
    ON metadata_catalogue.versioningstatus (uid);
CREATE INDEX IF NOT EXISTS idx_authorization_group_meta_id
    ON usergroup_catalogue.authorization_group (meta_id);

-- Reverse lookup indexes for targeted deletion of common values.
CREATE INDEX IF NOT EXISTS idx_contactpoint_element_element
    ON metadata_catalogue.contactpoint_element (element_instance_id);
CREATE INDEX IF NOT EXISTS idx_distribution_element_element
    ON metadata_catalogue.distribution_element (element_instance_id);
CREATE INDEX IF NOT EXISTS idx_webservice_element_element
    ON metadata_catalogue.webservice_element (element_instance_id);
CREATE INDEX IF NOT EXISTS idx_organization_element_element
    ON metadata_catalogue.organization_element (element_instance_id);
CREATE INDEX IF NOT EXISTS idx_person_element_element
    ON metadata_catalogue.person_element (element_instance_id);
CREATE INDEX IF NOT EXISTS idx_operation_element_element
    ON metadata_catalogue.operation_element (element_instance_id);
CREATE INDEX IF NOT EXISTS idx_mapping_element_element
    ON metadata_catalogue.mapping_element (element_instance_id);
CREATE INDEX IF NOT EXISTS idx_softwaresourcecode_element_element
    ON metadata_catalogue.softwaresourcecode_element (element_instance_id);
CREATE INDEX IF NOT EXISTS idx_equipment_element_element
    ON metadata_catalogue.equipment_element (element_instance_id);
CREATE INDEX IF NOT EXISTS idx_facility_element_element
    ON metadata_catalogue.facility_element (element_instance_id);

CREATE INDEX IF NOT EXISTS idx_dataproduct_identifier_identifier
    ON metadata_catalogue.dataproduct_identifier (identifier_instance_id);
CREATE INDEX IF NOT EXISTS idx_webservice_identifier_identifier
    ON metadata_catalogue.webservice_identifier (identifier_instance_id);
CREATE INDEX IF NOT EXISTS idx_organization_identifier_identifier
    ON metadata_catalogue.organization_identifier (identifier_instance_id);
CREATE INDEX IF NOT EXISTS idx_person_identifier_identifier
    ON metadata_catalogue.person_identifier (identifier_instance_id);
CREATE INDEX IF NOT EXISTS idx_facility_address_address
    ON metadata_catalogue.facility_address (address_instance_id);
CREATE INDEX IF NOT EXISTS idx_softwareapplication_parameter_parameter
    ON metadata_catalogue.softwareapplication_parameters (parameter_instance_id);

CREATE INDEX IF NOT EXISTS idx_dataproduct_spatial_spatial
    ON metadata_catalogue.dataproduct_spatial (spatial_instance_id);
CREATE INDEX IF NOT EXISTS idx_webservice_spatial_spatial
    ON metadata_catalogue.webservice_spatial (spatial_instance_id);
CREATE INDEX IF NOT EXISTS idx_facility_spatial_spatial
    ON metadata_catalogue.facility_spatial (spatial_instance_id);
CREATE INDEX IF NOT EXISTS idx_equipment_spatial_spatial
    ON metadata_catalogue.equipment_spatial (spatial_instance_id);
CREATE INDEX IF NOT EXISTS idx_service_spatial_spatial
    ON metadata_catalogue.service_spatial (spatial_instance_id);

CREATE INDEX IF NOT EXISTS idx_dataproduct_temporal_temporal
    ON metadata_catalogue.dataproduct_temporal (temporal_instance_id);
CREATE INDEX IF NOT EXISTS idx_webservice_temporal_temporal
    ON metadata_catalogue.webservice_temporal (temporal_instance_id);
CREATE INDEX IF NOT EXISTS idx_equipment_temporal_temporal
    ON metadata_catalogue.equipment_temporal (temporal_instance_id);
CREATE INDEX IF NOT EXISTS idx_service_temporal_temporal
    ON metadata_catalogue.service_temporal (temporal_instance_id);
