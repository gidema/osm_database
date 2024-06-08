package nl.osmdata;

import java.util.List;

public interface SchemaHelper {

    public List<String> getTables();

    public String getBasicSchemaDdl();

    public String getCreatePrimaryKeysDdl();

    public String getCreateSimpleIndexesDdl();

    public String getCreateGeoIndexesDdl();

    public String getCreateFinalTasksDdl();

}
