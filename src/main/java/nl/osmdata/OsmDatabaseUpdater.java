package nl.osmdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.inject.Inject;
import nl.osmdata.geofabrik.GeofabrikService;

@SpringBootApplication
@EnableJpaRepositories
public class OsmDatabaseUpdater implements CommandLineRunner {

    private static Logger LOG = LoggerFactory
            .getLogger(OsmDatabaseUpdater.class);

    @Inject
    private GeofabrikService service;

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        LOG.info("STARTING THE APPLICATION");
        try {
            SpringApplication.run(OsmDatabaseUpdater.class, args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
//            LOG.error(e.getMessage(), e);
        }
        LOG.info("APPLICATION FINISHED");
    }

    @Override
    public void run(String ... args) {
        LOG.info("EXECUTING : OSM database updater");
        service.updateDatabase();
    }

}
