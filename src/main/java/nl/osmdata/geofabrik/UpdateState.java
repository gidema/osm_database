package nl.osmdata.geofabrik;

import java.time.ZonedDateTime;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode

public class UpdateState {
    private ZonedDateTime timeStamp;
    private Long sequenceNumber;
}
