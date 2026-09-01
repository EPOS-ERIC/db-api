-- Optional audit for existing databases. The application remains functional
-- without running this script; returned rows identify legacy data to clean up.
SELECT meta_id, uid, status, COUNT(*) AS version_count,
       ARRAY_AGG(instance_id ORDER BY instance_id) AS instance_ids
FROM metadata_catalogue.versioningstatus
WHERE meta_id IS NOT NULL AND uid IS NOT NULL
  AND status IN ('DRAFT', 'SUBMITTED')
GROUP BY meta_id, uid, status
HAVING COUNT(*) > 1
ORDER BY meta_id, status;

-- The policy also forbids a DRAFT and SUBMITTED pair for the same metadata.
SELECT meta_id, uid,
       COUNT(*) FILTER (WHERE status = 'DRAFT') AS draft_count,
       COUNT(*) FILTER (WHERE status = 'SUBMITTED') AS submitted_count
FROM metadata_catalogue.versioningstatus
WHERE meta_id IS NOT NULL AND uid IS NOT NULL
  AND status IN ('DRAFT', 'SUBMITTED')
GROUP BY meta_id, uid
HAVING COUNT(*) FILTER (WHERE status = 'DRAFT') > 0
   AND COUNT(*) FILTER (WHERE status = 'SUBMITTED') > 0
ORDER BY meta_id;
