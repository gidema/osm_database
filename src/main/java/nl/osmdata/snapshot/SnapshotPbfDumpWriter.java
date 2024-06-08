package nl.osmdata.snapshot;

import java.io.File;

import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;
import org.openstreetmap.osmosis.pgsnapshot.common.NodeLocationStoreType;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.PostgreSqlDumpWriter;

import nl.osmdata.PbfDumpWriter;

public class SnapshotPbfDumpWriter implements PbfDumpWriter {
    private final static int workers = 4; 
    
    @Override
    public void writeDump(File inputFile, File outputFolder) {
        var pbfReader = new PbfReader(inputFile, workers);
        try (var dumpWriter = new PostgreSqlDumpWriter(outputFolder, false, false, false, NodeLocationStoreType.TempFile, false)) {
            pbfReader.setSink(dumpWriter);
        }
        pbfReader.run();
    }
}
