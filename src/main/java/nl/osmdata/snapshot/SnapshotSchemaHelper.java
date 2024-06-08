package nl.osmdata.snapshot;

import java.util.List;
import nl.osmdata.SchemaHelper;

public class SnapshotSchemaHelper implements SchemaHelper {
    private static List<String> tables = List.of(
//            "actions",
            "users",
            "nodes",
            "ways",
            "way_nodes",
            "relations",
            "relation_members");   
    
   private static final String basicSchemaDdl = """
-- Database creation script for the simple PostgreSQL schema.

-- Drop all tables if they exist.
DROP TABLE IF EXISTS actions;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS nodes;
DROP TABLE IF EXISTS ways;
DROP TABLE IF EXISTS way_nodes;
DROP TABLE IF EXISTS relations;
DROP TABLE IF EXISTS relation_members;
DROP TABLE IF EXISTS schema_info;
DROP TABLE IF EXISTS replication_changes;
DROP TABLE IF EXISTS sql_changes;

-- Drop all stored procedures if they exist.
DROP FUNCTION IF EXISTS osmosisUpdate();

-- Create a table which will contain a single row defining the current schema version.
CREATE TABLE schema_info (
    version integer NOT NULL
);


-- Create a table for users.
CREATE TABLE users (
    id int NOT NULL,
    name text NOT NULL
);


-- Create a table for nodes.
CREATE TABLE nodes (
    id bigint NOT NULL,
    version int NOT NULL,
    user_id int NOT NULL,
    tstamp timestamp without time zone NOT NULL,
    changeset_id bigint NOT NULL,
    tags hstore
);
-- Add a postgis point column holding the location of the node.
SELECT AddGeometryColumn('nodes', 'geom', 4326, 'POINT', 2);


-- Create a table for ways.
CREATE TABLE ways (
    id bigint NOT NULL,
    version int NOT NULL,
    user_id int NOT NULL,
    tstamp timestamp without time zone NOT NULL,
    changeset_id bigint NOT NULL,
    tags hstore,
    nodes bigint[]
);


-- Create a table for representing way to node relationships.
CREATE TABLE way_nodes (
    way_id bigint NOT NULL,
    node_id bigint NOT NULL,
    sequence_id int NOT NULL
);


-- Create a table for relations.
CREATE TABLE relations (
    id bigint NOT NULL,
    version int NOT NULL,
    user_id int NOT NULL,
    tstamp timestamp without time zone NOT NULL,
    changeset_id bigint NOT NULL,
    tags hstore
);

-- Create a table for representing relation member relationships.
CREATE TABLE relation_members (
    relation_id bigint NOT NULL,
    member_id bigint NOT NULL,
    member_type character(1) NOT NULL,
    member_role text NOT NULL,
    sequence_id int NOT NULL
);

-- Create a table for replication changes that are applied to the database.
CREATE TABLE replication_changes (
    id SERIAL,
    tstamp TIMESTAMP without time zone NOT NULL DEFAULT(NOW()),
    nodes_modified INT NOT NULL DEFAULT (0),
    nodes_added INT NOT NULL DEFAULT (0),
    nodes_deleted INT NOT NULL DEFAULT (0),
    ways_modified INT NOT NULL DEFAULT (0),
    ways_added INT NOT NULL DEFAULT (0),
    ways_deleted INT NOT NULL DEFAULT (0),
    relations_modified INT NOT NULL DEFAULT (0),
    relations_added INT NOT NULL DEFAULT (0),
    relations_deleted INT NOT NULL DEFAULT (0),
    changesets_applied BIGINT [] NOT NULL,
    earliest_timestamp TIMESTAMP without time zone NOT NULL,
    latest_timestamp TIMESTAMP without time zone NOT NULL
);

CREATE TABLE sql_changes (
  id SERIAL,
  tstamp TIMESTAMP without time zone NOT NULL DEFAULT(NOW()),
  entity_id BIGINT NOT NULL,
  type TEXT NOT NULL,
  changeset_id BIGINT NOT NULL,
  change_time TIMESTAMP NOT NULL,
  action INT NOT NULL,
  query text NOT NULL,
  arguments text
);


-- Configure the schema version.
INSERT INTO schema_info (version) VALUES (6);""";

   private static final String primaryKeysDdl = """
-- Add primary keys to tables.
ALTER TABLE ONLY schema_info ADD CONSTRAINT pk_schema_info PRIMARY KEY (version);

ALTER TABLE ONLY users ADD CONSTRAINT pk_users PRIMARY KEY (id);

ALTER TABLE ONLY nodes ADD CONSTRAINT pk_nodes PRIMARY KEY (id);

ALTER TABLE ONLY ways ADD CONSTRAINT pk_ways PRIMARY KEY (id);

ALTER TABLE ONLY way_nodes ADD CONSTRAINT pk_way_nodes PRIMARY KEY (way_id, sequence_id);

ALTER TABLE ONLY relations ADD CONSTRAINT pk_relations PRIMARY KEY (id);

ALTER TABLE ONLY relation_members ADD CONSTRAINT pk_relation_members PRIMARY KEY (relation_id, sequence_id);""";

   private static final String simpleIndexesDdl = """

-- Add simple indexes to tables.
CREATE INDEX idx_way_nodes_node_id ON way_nodes USING btree (node_id);

CREATE INDEX idx_relation_members_member_id_and_type ON relation_members USING btree (member_id, member_type);""";
   
   private static final String geoIndexesDdl = """

-- Add geo index(es) to tables.
CREATE INDEX idx_nodes_geom ON nodes USING gist (geom);""";

   private static final String finalTasksDdl = """

-- Create customisable hook function that is called within the replication update transaction.
CREATE FUNCTION osmosisUpdate() RETURNS void AS $$
DECLARE
BEGIN
END;
$$ LANGUAGE plpgsql;""";

    @Override
    public List<String> getTables() {
        return tables;
    }

    @Override
    public String getBasicSchemaDdl() {
        return basicSchemaDdl;
    }
    
    @Override
    public String getCreatePrimaryKeysDdl() {
        return primaryKeysDdl;
    }
    
    @Override
    public String getCreateSimpleIndexesDdl() {
        return simpleIndexesDdl;
    }
    
    @Override
    public String getCreateGeoIndexesDdl() {
        return geoIndexesDdl;
    }

    @Override
    public String getCreateFinalTasksDdl() {
        return finalTasksDdl;
    }
}    

