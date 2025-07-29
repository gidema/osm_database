package nl.osmdata.geofabrik;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
import org.openstreetmap.osmosis.core.database.DatabaseType;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import nl.osmdata.snapshot.SnapshotChangeHandler;

@Component
public class GeofabrikService {
    
    @Value("${geofabrik.server.root}")
    private String serverRoot;
    @Value("${geofabrik.update.state_file}")
    private String stateFileName;
    @Value("${geofabrik.continent}")
    private String continent;
    @Value("${geofabrik.country}")
    private String country;
    @Value("${temp_folder}")
    private Path tempPath;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;

    
    @Inject
    private GeofabrikChangeSetRepository changeSetRepository;
    
    /**
     * Update the Osm database to include the changes from the latest Geofabrik
     * changeSet files for the current country;
     */
    public void updateDatabase() {
        var statePath = downloadCountryStateFile();
        var currentState = parseState(statePath);
        var lastChangeSet = changeSetRepository.getLastChangeSet();
        if (lastChangeSet == null) {
            throw new RuntimeException("The sequence number of the last changeSet is not known. Code" + 
                    "to handle this issue hasn't been implemented yet");
        }
        var firstNewChangeSet = lastChangeSet.getSequenceNumber() + 1;
        var lastNewChangeSet = currentState.getSequenceNumber().intValue();
        for (int sequenceNumber = firstNewChangeSet; sequenceNumber<=lastNewChangeSet; sequenceNumber++) {
            processChangeSet(sequenceNumber);
        }
    }

    private DatabaseLoginCredentials getCredentials() {
        var parts = datasourceUrl.split(":");
        var database = parts[2];
        DatabaseType dbType = DatabaseType.POSTGRESQL;
        switch (parts[1].toLowerCase()) {
        case "postgresql":
            dbType = DatabaseType.POSTGRESQL;
            break;
        case "mysql":
            dbType = DatabaseType.MYSQL;
            break;
        default:
            throw new RuntimeException(String.format(
                "Unsupported database type: %s.", parts[1]));
        }
        return new DatabaseLoginCredentials("localhost", database, username, password, true, false, dbType);
    }

    private void processChangeSet(int sequenceNumber) {
        var changeSet = changeSetRepository.findBySequenceNumber(sequenceNumber);
        if (changeSet == null) {
            changeSet = new GeofabrikChangeSet(continent, country, sequenceNumber);
        }
        changeSet = changeSetRepository.save(changeSet);
        Path stateFile;
        Path changeSetFile;
        try {
            stateFile = downloadChangeSetStateFile(sequenceNumber);
            var updateState = parseState(stateFile);
            changeSetFile = downloadChangeSetFile(sequenceNumber);
            var attributes = Files.readAttributes(changeSetFile, BasicFileAttributes.class);
            var timestamp = ZonedDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.of("GMT"));
            changeSet.setDownloadTimestamp(timestamp);
            changeSet.setFileTimestamp(updateState.getTimeStamp());
            changeSet.setStatus("downloaded");
            changeSetRepository.save(changeSet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            var dbCredentials = getCredentials();
            var changeHandler = new SnapshotChangeHandler(CompressionMethod.GZip);
            changeHandler.writeChange(changeSetFile.toFile(), dbCredentials);
            changeSet.setStatus("updated");
            changeSetRepository.save(changeSet);
        } catch (OsmosisRuntimeException e) {
            throw e;
        }
    }

    /**
     * Download a file containing the state for the current country (state.txt)
     * and save it in a temporary location.
     * 
     * @return The path to the downloaded state file.
     */
    private Path downloadCountryStateFile() {
        var source = String.format("http://%s/%s/%s-updates/%s", 
                serverRoot, continent, country, stateFileName);
        var uri = createURI(source);
        var targetName = String.format("%s/%s/%s", continent, country, stateFileName);
        var targetPath = tempPath.resolve(targetName);
        return downloadFile(uri, targetPath);
    }
    
    /**
     * Download a file containing the state for change set (state.txt)
     * and save it in a temporary location.
     * 
     * @return The path to the downloaded state file.
     */
    private Path downloadChangeSetStateFile(Integer sequenceNumber) {
        long units = Math.floorMod(sequenceNumber, 1000);

        var source = String.format("%s/%03d.state.txt", getSourceFolder(sequenceNumber), units);
        var uri = createURI(source);
        var targetName =String.format("%s/%s-%09d.state.txt", continent,
                country, sequenceNumber);
        var targetPath = tempPath.resolve(targetName);
        return downloadFile(uri, targetPath);
    }

    private Path downloadChangeSetFile(Integer sequenceNumber) {
        long units = Math.floorMod(sequenceNumber, 1000);

        var source = String.format("%s/%03d.osc.gz", getSourceFolder(sequenceNumber), units);
        var uri = createURI(source);
        var targetName =String.format("%s/%s-%09d.osc.gz", continent,
                country, sequenceNumber);
        var targetPath = tempPath.resolve(targetName);
        return downloadFile(uri, targetPath);
    }

    private static Path downloadFile(URI uri, Path targetPath) {
//        HttpResponse<Path> response;
        HttpResponse<InputStream> response;
        try (HttpClient client = HttpClient.newBuilder().build()) {
            var request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml,application/x-gzip;q=0.9,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip")
                    .GET()
                    .build();
            Files.createDirectories(targetPath.getParent());
//            response = client.send(request, BodyHandlers.ofFile(targetPath));
            var bodyHandler = BodyHandlers.ofInputStream();
            response = client.send(request, bodyHandler);
            switch (response.statusCode()) {
            case 404:
//                Files.delete(response.body());
                throw new RuntimeException(String.format("The requested file (%s) could not be found", uri.toString()));
            case 200: {
//                return response.body();
                InputStream is;
                var contentEncoding = response.headers().firstValue("Content-Encoding").orElse("none");
                switch (contentEncoding) {
                case "none":
                    is = response.body();
                    break;
                case "gzip":
                    is = new GZIPInputStream(response.body());
                    break;
                default:
                    throw new RuntimeException(String.format("Unkown content encoding: %s", contentEncoding));
                }
                try (
                    var os = new FileOutputStream(targetPath.toFile());
                ) {
                    is.transferTo(os);
                    is.close();
                }
                return targetPath;
            }
            default:
                throw new RuntimeException();
            }
        } catch (@SuppressWarnings("unused") InterruptedException e) {
            throw new RuntimeException(String.format("The download of (%s) was interrupted unexpectedly.", uri));
        } catch (IOException e) {
            throw new RuntimeException(String.format("The download of (%s) failed with exception:%s.", uri, e.getMessage()));
        }
    }
    
    
    public static UpdateState parseState(Path stateFile) {
        var state = new UpdateState();
        String key = "unknown";
        String value = "unknown";
        try (
            FileReader reader = new FileReader(stateFile.toFile());)
        {
            var stateProperties = new Properties();
            stateProperties.load(reader);
            key = "sequenceNumber";
            value = (String) stateProperties.get(key);
            state.setSequenceNumber(Long.valueOf(value));
            key = "timestamp";
            value = (String) stateProperties.get(key);
            state.setTimeStamp(ZonedDateTime.parse(value));
            return state;
        } catch (ClassCastException | NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Illegal argument %s for parameter %s.", e , value, key));
        }
    catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static URI createURI(String path) {
        try {
            return new URI(path);
        } catch (@SuppressWarnings("unused") URISyntaxException e) {
            throw new RuntimeException(String.format("The source path (%s) is not a valid URI.", path));
        }
    }
    
    private String getSourceFolder(Integer sequenceNumber) {
        long millions = Math.floorDiv(sequenceNumber, 1000000);
        long thousands = Math.floorMod(Math.floorDiv(sequenceNumber, 1000), 1000);
        long units = Math.floorMod(sequenceNumber, 1000);

        return String.format("http://%s/%s/%s-updates/%03d/%03d", 
                serverRoot, continent, country, millions, thousands, units);
    }
}
