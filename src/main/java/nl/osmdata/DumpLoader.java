package nl.osmdata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Write the data for a Postgresql schema to the database
 */
public class DumpLoader {
    private Logger logger = LoggerFactory.getLogger(DumpLoader.class);
    
    private final String dbUrl;
    private final Properties properties;
    private Connection conn;
    private final SchemaHelper schemaHelper;
    
    public DumpLoader(String dbUrl, Properties properties, SchemaHelper schemaHelper) {
        super();
        this.dbUrl = dbUrl;
        this.properties = properties;
        this.schemaHelper=schemaHelper;
    }

    public void load(File sourcePath) {
        conn = createConnection();
        createSchema();
        writeTables(sourcePath);
        createPrimaryKeys();
        createSimpleIndexes();
        createGeoIndexes();
        executeFinalDdlTasks();
    }

    private Connection createConnection() {
        try {
            return DriverManager.getConnection(dbUrl, properties);
        } catch (SQLException e) {
            throw new RuntimeException(String.format(
                "Could not create connection to database %s for user %s.", dbUrl, properties.get("user")), e);
        }
    }
    
    private void createSchema() {
        try {
            String ddl = schemaHelper.getBasicSchemaDdl();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(ddl);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Couldn't create the Postgresql database schema", e);
        }
    }
    
    private void writeTables(File sourcePath) {
        schemaHelper.getTables().forEach(table -> {
            try {
                Instant start = Instant.now();
                long rows = writeTable(sourcePath, table);
                long timeElapsed = Duration.between(start, Instant.now()).toMillis();
                logger.info("Loaded table '{}' ({} rows) in {} ms", table, rows, timeElapsed);
            } catch (IOException | SQLException e) {
                throw new RuntimeException("Couldn't write the Postgresql database tables.", e);
            }
        });
    }
    
    private long writeTable(File sourcePath, String tableName) throws FileNotFoundException, IOException, SQLException {
        File inputDataFile = new File(sourcePath, tableName + ".txt");
        String sql = String.format("COPY %s FROM stdin", tableName);
        BaseConnection pgcon = (BaseConnection)conn;
        CopyManager mgr = new CopyManager(pgcon);
        try (Reader in = new BufferedReader(new FileReader(inputDataFile))) {
            return mgr.copyIn(sql, in);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Could not create Postgresql table %s from dump file.", tableName), e);
        }
    }
    

    private void createPrimaryKeys() {
        try {
            String ddl = schemaHelper.getCreatePrimaryKeysDdl();
            try (Statement stmt = conn.createStatement()) {
                Instant start = Instant.now();
                stmt.execute(ddl);
                logger.info("Created primary keys in {} s", Duration.between(start, Instant.now()).getSeconds());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Couldn't create the Postgresql primary keys", e);
        }
    }
    
    private void createSimpleIndexes() {
        try {
            String ddl = schemaHelper.getCreateSimpleIndexesDdl();
            try (Statement stmt = conn.createStatement()) {
                Instant start = Instant.now();
                stmt.execute(ddl);
                logger.info("Created simple indexes in {} s", Duration.between(start, Instant.now()).getSeconds());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Couldn't create the Postgresql simple indexes", e);
        }
    }

    private void createGeoIndexes() {
        try {
            String ddl = schemaHelper.getCreateGeoIndexesDdl();
            try (Statement stmt = conn.createStatement()) {
                Instant start = Instant.now();
                stmt.execute(ddl);
                logger.info("Created geo indexes in {} s", Duration.between(start, Instant.now()).getSeconds());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Couldn't create the Postgresql geo index(es)", e);
        }
    }

    private void executeFinalDdlTasks() {
        try {
            String ddl = schemaHelper.getCreateFinalTasksDdl();
            try (Statement stmt = conn.createStatement()) {
                Instant start = Instant.now();
                stmt.execute(ddl);
                logger.info("Executed final DDL tasks in {} s", Duration.between(start, Instant.now()).getSeconds());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Couldn't execute the final DDL tasks.", e);
        }
    }

}
