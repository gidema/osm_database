package nl.osmdata;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsmCountryDownloader {
    private Logger logger = LoggerFactory.getLogger(OsmCountryDownloader.class);
    
    private File tempDir;
    private final PbfDumpWriter dumpWriter;
    private final DumpLoader dumpLoader;


    public OsmCountryDownloader(PbfDumpWriter dumpWriter, DumpLoader dumpLoader) {
        super();
        this.dumpWriter = dumpWriter;
        this.dumpLoader = dumpLoader;
    }

    public void download(String url) {
        tempDir = createTempDir();
//        tempDir = new File("/tmp/osmdata7836210196246711327");
        File pbfFile = downloadPbfFile(url);
        createPostgresDumpfiles(pbfFile);
        loadDumpFiles();
    }

    // Create a directory for temporary data
    /**
     * Create a temporary directory in the filesystem's default temp directory
     */
    private File createTempDir() {
        try {
            File dir = Files.createTempDirectory("osmdata").toFile();
            logger.info("Created temporary directory '{}'", dir);
            return dir;
        } catch (IOException e) {
            throw new RuntimeException("Could not create a temporary directory", e);
        }
    }
    
    /**
     * Download the Pbf datadump
     * 
     * @param url The network location of the pbf file
     * @return The file location of the downloaded pbf file
     */
    private File downloadPbfFile(String url) {
        File pbfFile = null;
        try {
            Instant start = Instant.now();
            var pbfDownloader = new PbfDownloader();
            pbfFile = File.createTempFile("osmdata", ".pbf", tempDir); 
            pbfDownloader.download(new URL(url), pbfFile);
            logger.info("Downloaded pbf file in {}s", Duration.between(start, Instant.now()).getSeconds());
            return pbfFile;
        } catch (IOException e) {
            throw new RuntimeException("Could not download the PBF file with OSM data.", e);
        }
     }

    
    /**
     * Create a set of txt file that can be read by the Postgresql database server
     */
    private void createPostgresDumpfiles(File pbfFile) {
        Instant start = Instant.now();
        dumpWriter.writeDump(pbfFile, tempDir);
        logger.info("Created postgres dump file in {}s", Duration.between(start, Instant.now()).getSeconds());
    }

    private void loadDumpFiles() {
        Instant start = Instant.now();
        dumpLoader.load(tempDir);
        logger.info("Loaded dump files to server in {}s", Duration.between(start, Instant.now()).getSeconds());
    }
}
