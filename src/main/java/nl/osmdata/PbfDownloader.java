package nl.osmdata;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class PbfDownloader {
    
    @SuppressWarnings("static-method")
    public void download(URL url, File outputFile) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        
        try (
                BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
        ){
            in.transferTo(out);
        }
    }
}
