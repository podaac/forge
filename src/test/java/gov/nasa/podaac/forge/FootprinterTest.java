package gov.nasa.podaac.forge;

import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import gov.nasa.podaac.forge.pojo.DatasetConfig;
import gov.nasa.podaac.forge.pojo.FootprintConfig;
import gov.nasa.podaac.forge.strategy.FootprintStrategy;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class FootprinterTest extends TestCase {
    
    /**
     * Parsing a dataset config should return a DatasetConfig object containing the expected values.
     */
    @Test
    public void testParseConfig() throws FileNotFoundException {
        String configFilePath = this.getClass().getResource("/MODIS_T/PODAAC-GHMDT-2PJ19.cfg").getPath();
        Footprinter footprinter = new Footprinter();
        DatasetConfig datasetConfig = footprinter.parseConfig(configFilePath);
        FootprintConfig footprintConfig = datasetConfig.getFootprint();
        assert datasetConfig.getLatVar().equals("lat");
        assert datasetConfig.getLonVar().equals("lon");
        assert !datasetConfig.isIs360();
        assert datasetConfig.getTolerance() == 0.5;
        assert footprintConfig.getStrategy() == FootprintStrategy.Strategy.PERIODIC;
        assert footprintConfig.getTop().equals("0:0,0:*");
        assert footprintConfig.getBottom().equals("*:*,0:*");
        assert footprintConfig.getSide1().equals("0:*,0:0");
        assert footprintConfig.getSide2().equals("0:*,*:*");
    }
    
    /**
     * Parsing a dataset config with invalid JSON (in this case, remove a curly brace) should retsult in a
     * JsonSyntaxException being thrown
     */
    @Test
    public void testParseConfigJsonParseError(@TempDir Path tempDir) throws IOException {
        String configFileContents = IOUtils.toString(
                this.getClass().getResourceAsStream("/MODIS_T/PODAAC-GHMDT-2PJ19.cfg"),
                StandardCharsets.UTF_8
        ).replaceAll("\\s+", "");
        
        configFileContents = configFileContents.replaceFirst("}", "");
        
        Path path = tempDir.resolve("config.cfg");
        FileWriter fw = new FileWriter(path.toString());
        BufferedWriter bw = new BufferedWriter(fw);
        // Write bad contents to temp file file
        bw.write(configFileContents);
        // Close connection
        bw.close();
        
        // parsing config should fail
        Footprinter footprinter = new Footprinter();
        Assertions.assertThrows(JsonSyntaxException.class, () -> footprinter.parseConfig(path.toString()));
    }
    
    /**
     * Parsing a dataset config with a missing required field (in this case, latVar is missing) should result in a
     * JsonParseException exception being thrown.
     */
    @Test
    public void testParseConfigMissingRequiredFields(@TempDir Path tempDir) throws IOException {
        String configFileContents = IOUtils.toString(
                this.getClass().getResourceAsStream("/MODIS_T/PODAAC-GHMDT-2PJ19.cfg"),
                StandardCharsets.UTF_8
        ).replaceAll("\\s+", "");
        
        configFileContents = configFileContents.replaceFirst("\"latVar\":\"lat\",", "");
    
        Path path = tempDir.resolve("config.cfg");
        FileWriter fw = new FileWriter(path.toString());
        BufferedWriter bw = new BufferedWriter(fw);
        // Write bad contents to temp file file
        bw.write(configFileContents);
        // Close connection
        bw.close();
        
        // parsing config should fail
        Footprinter footprinter = new Footprinter();
        Assertions.assertThrows(JsonParseException.class, () -> footprinter.parseConfig(path.toString()));
    }
}
