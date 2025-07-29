package nl.osmdata.geofabrik;

import java.time.ZonedDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class GeofabrikChangeSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String continent;
    private String country;
    private Integer sequenceNumber;
    private ZonedDateTime fileTimestamp;
    private ZonedDateTime downloadTimestamp;
    private String status;
    
    public GeofabrikChangeSet() {
    }
    public GeofabrikChangeSet(String continent, String country, Integer sequenceNumber) {
        super();
        this.continent = continent;
        this.country = country;
        this.sequenceNumber = sequenceNumber;
    }
}


