package nl.osmdata.snapshot;

import java.io.File;

import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
import org.openstreetmap.osmosis.core.database.DatabasePreferences;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.PostgreSqlChangeWriter;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlChangeReader;

public class SnapshotChangeHandler {
    private final CompressionMethod compressionMethod;
    
    public SnapshotChangeHandler(CompressionMethod compressionMethod) {
        super();
        this.compressionMethod = compressionMethod;
    }

    public void writeChange(File inputFile, DatabaseLoginCredentials dbCredentials) {
        DatabasePreferences dbPreferences = new DatabasePreferences(false, false);
        var changeReader = new XmlChangeReader(inputFile, false, compressionMethod);
        try (var changeWriter = new PostgreSqlChangeWriter(dbCredentials, dbPreferences, false, false)) {
            changeReader.setChangeSink(changeWriter);
            changeReader.run();
        }
    }
}
