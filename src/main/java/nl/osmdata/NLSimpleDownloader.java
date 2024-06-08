package nl.osmdata;

import java.util.Properties;

import nl.osmdata.pgsimple.SimplePbfDumpWriter;
import nl.osmdata.pgsimple.SimpleSchemaHelper;

public class NLSimpleDownloader {
    private final static String url = "https://download.geofabrik.de/europe/netherlands-latest.osm.pbf"; 
    private final static String dbUrl = "jdbc:postgresql://localhost/nlgis";

    public static void main(String[] args) {
        var properties = new Properties();
        properties.setProperty("user", "nlgis");
        properties.setProperty("password", "nlgis");
        DumpLoader dumpLoader = new DumpLoader(dbUrl, properties, new SimpleSchemaHelper());
        var downloader = new OsmCountryDownloader(new SimplePbfDumpWriter(), dumpLoader);
        downloader.download(url);
    }
}
