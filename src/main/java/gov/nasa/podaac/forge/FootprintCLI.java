import gov.nasa.podaac.forge.Footprinter;
import java.util.Map;

/**
 * A simple command-line interface for the Footprinter class.
 */
class FootprintCLI {

    public FootprintCLI() {
        // Constructor, if needed
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java FootprintCLI <granuleFile> <configFile>");
            System.exit(1);
        }

        String granuleFile = args[0];
        String configFile = args[1];

        try {
            System.out.println("Processing File");
            Footprinter footprinter = new Footprinter(granuleFile, configFile);
            Map<String, String> fp = footprinter.footprint();
            System.out.println("Process retrieved footprint");
            System.out.println(fp.get("FOOTPRINT"));
        } catch (Exception e) {
            System.err.println("Error processing: " + granuleFile);
            // Log the exception or handle it appropriately
            e.printStackTrace(System.err);
        }
    }
}
