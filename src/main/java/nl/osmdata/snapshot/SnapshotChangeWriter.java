package nl.osmdata.snapshot;

import org.openstreetmap.osmosis.pgsnapshot.v0_6.PostgreSqlChangeWriter;

public class SnapshotChangeWriter {
    public void run() {
        new PostgreSqlChangeWriter(null, null, false, false);
    }
}
