package gov.nasa.podaac.forge;

import java.util.Map;
import gov.nasa.podaac.forge.Footprinter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Main {
    
        public static void main (String[] args) throws Exception {
        String filePath = "../../scripts/SWOT_L2_LR_SSH_Basic_001_001_20160901T000000_20160901T005126_DG10_01.nc";
        String configFilePath = "../forge-tig-configuration/config-files/SWOT_L2_LR_SSH_BASIC_1.0.cfg";

        Footprinter footprinter = new Footprinter(filePath, configFilePath);
        Map<String, String> footprint = footprinter.footprint();
        System.out.println(footprint);
    }
} 

/*
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!"); 
    }
}
*/