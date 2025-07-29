package nl.osmdata.geofabrik;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface GeofabrikChangeSetRepository extends CrudRepository<GeofabrikChangeSet, Long> {
    static final String LAST_UPDATE_SQL = """
SELECT gcs.* 
FROM geofabrik_change_set gcs
WHERE gcs.status='updated'
ORDER BY gcs.sequence_number DESC
LIMIT 1
""";
    
    @Query(value = LAST_UPDATE_SQL, nativeQuery = true)
    GeofabrikChangeSet getLastChangeSet();
    
    GeofabrikChangeSet findBySequenceNumber(Integer sequenceNumber);
}
