package nl.osmdata;

import java.io.File;

public interface PbfDumpWriter {

    public void writeDump(File inputFile, File outputFolder);

}
