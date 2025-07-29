package nl.osmdata.geofabrik;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class Country {
    private String country;
    private String continent;
}
