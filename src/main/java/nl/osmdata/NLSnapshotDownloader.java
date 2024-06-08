package nl.osmdata;

import java.util.Properties;

import nl.osmdata.snapshot.SnapshotPbfDumpWriter;
import nl.osmdata.snapshot.SnapshotSchemaHelper;

public class NLSnapshotDownloader {
    private final static String url = "https://download.geofabrik.de/europe/netherlands-latest.osm.pbf"; 
    private final static String dbUrl = "jdbc:postgresql://localhost/nlgis";

    public static void main(String[] args) {
        var properties = new Properties();
        properties.setProperty("user", "nlgis");
        properties.setProperty("password", "nlgis");
        DumpLoader dumpLoader = new DumpLoader(dbUrl, properties, new SnapshotSchemaHelper());
        var downloader = new OsmCountryDownloader(new SnapshotPbfDumpWriter(), dumpLoader);
        downloader.download(url);
    }
}
