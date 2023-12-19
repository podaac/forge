import gov.nasa.podaac.forge.Footprinter;
import java.util.Map;
import java.io.FileWriter;
import java.io.IOException;

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
        String footprint = "";
        String filePath = granuleFile + ".fp";

        try {
            System.out.println("Processing File");
            Footprinter footprinter = new Footprinter(granuleFile, configFile);
            Map<String, String> fp = footprinter.footprint();
            System.out.println("Process retrieved footprint");
            System.out.println(fp.get("FOOTPRINT"));
            footprint = fp.get("FOOTPRINT");
        } catch (Exception e) {
            System.err.println("Error processing: " + granuleFile);
            // Log the exception or handle it appropriately
            e.printStackTrace(System.err);
        }

        try {
            // Create a FileWriter object with the specified file path
            FileWriter fileWriter = new FileWriter(filePath);

            // Use the write method to write the string to the file
            fileWriter.write(footprint);

            // Close the FileWriter to release resources
            fileWriter.close();

            System.out.println("String has been written to the file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
