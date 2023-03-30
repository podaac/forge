package gov.nasa.podaac.forge;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class FootprintTestUtil {
    
    /**
     * Helper function used to generate coord lists
     */
    public static List<Coordinate> genList(double lonStart, double latStart, double lonStop, double latStop, double points) {
        List<Coordinate> coords = new ArrayList<>();
        
        double lonMag = Math.abs(lonStart - lonStop);
        double latMag = Math.abs(latStart - latStop);
        
        if (latStart > latStop)
            latMag = latMag * -1;
        
        if (lonStart > lonStop)
            lonMag = lonMag * -1;
        
        double lonStep = lonMag / points;
        double latStep = latMag / points;
        
        for (int i = 0; i <= points; i++) {
            double lat = latStart + (i * latStep);
            double lon = lonStart + (i * lonStep);
            
            while (lon > 180)
                lon = lon - 360;
            
            coords.add(new Coordinate(lon, lat));
        }
        
        return coords;
    }
}
