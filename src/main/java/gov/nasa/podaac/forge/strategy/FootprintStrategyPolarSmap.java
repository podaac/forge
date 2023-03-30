package gov.nasa.podaac.forge.strategy;

import com.vividsolutions.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Footprint strategy to be used by the {@link gov.nasa.podaac.forge.Footprinter} which specifies how the footprint
 * is generated. This strategy shares many common features with {@link FootprintStrategyPeriodic},
 * so it extends that class.
 */
public class FootprintStrategyPolarSmap extends FootprintStrategyPeriodic {
    
    private static final Logger log = LoggerFactory.getLogger(FootprintStrategyPolarSmap.class);
    
    public FootprintStrategyPolarSmap() {
        this.splitMargin = 179.99d;
    }
    
    /**
     * Add additional coordinates to the top/bottom/sides coordinate lists, where the value at
     * that coordinate is valid.
     *
     * @throws IOException If the NetCDF Lat/Lon variables cannot be read
     */
    public void calculateFootprint(Variable lonVariable, Variable latVariable,
                                   Map<String, Double> latAttMap, Map<String, Double> lonAttMap,
                                   List<Coordinate> side1, List<Coordinate> side2,
                                   List<Coordinate> top, List<Coordinate> bottom,
                                   boolean is360, boolean findValid, boolean removeOrigin) throws IOException {
        
        Array latData = latVariable.read();
        Array lonData = lonVariable.read();
        
        float[][][] latDataValues = (float[][][]) latData.copyToNDJavaArray();
        float[][][] lonDataValues = (float[][][]) lonData.copyToNDJavaArray();

        int size_z = latDataValues[0][0].length;
        int size_y = latDataValues[0].length;
        int size_x = latDataValues.length;

        float [][] latDataValuesFlat = new float[size_x][size_y];
        float [][] lonDataValuesFlat = new float[size_x][size_y];

        // Create a 2D array from the 3D array to make a more complete footprint
        for(int x = 0; x < size_x; x++){
            for(int y = 0; y < size_y; y++) {
                latDataValuesFlat[x][y] = latAttMap.get("fill").floatValue();
                lonDataValuesFlat[x][y] = lonAttMap.get("fill").floatValue();
                for(int z = 0; z < size_z; z++){
                    float latValue = latDataValues[x][y][z];
                    float lonValue = lonDataValues[x][y][z];
                    if (latValue != latAttMap.get("fill") && lonValue != lonAttMap.get("fill")){
                        latDataValuesFlat[x][y] = latValue;
                        lonDataValuesFlat[x][y] = lonValue;
                        break;
                    }
                }
            }
        }

        int bottomIndexPrev = -1;
        int topIndexPrev = -1;
        for (int col = 0; col < latDataValuesFlat[0].length; col++) {
            int topIndex = -1;
            int bottomIndex = -1;
            
            for (int row = 0; row < latDataValuesFlat.length; row++) {
                float latValue = latDataValuesFlat[row][col];
                float lonValue = lonDataValuesFlat[row][col];

                // Find the first value in the current column that is valid (not fill), and add that value to the
                // 'bottom' coordinates list.
                if (latValue != latAttMap.get("fill") && lonValue != lonAttMap.get("fill") && bottomIndex == -1) {
                    bottomIndex = row;
                    //bottom
                    addLonLat2List(bottom, latValue, lonValue, latAttMap, lonAttMap, is360, removeOrigin);
                }
                
                int reverseRow = latDataValues.length - row - 1;
                latValue = latDataValuesFlat[reverseRow][col];
                lonValue = lonDataValuesFlat[reverseRow][col];
                
                // Find the last value in the current column that is valid (not fill), and add that value to the
                // 'top' coordinates list.
                if (latValue != latAttMap.get("fill") && lonValue != lonAttMap.get("fill") && topIndex == -1) {
                    topIndex = reverseRow;
                    //top
                    addLonLat2List(top, latValue, lonValue, latAttMap, lonAttMap, is360, removeOrigin);
                }
            }
            
            if (bottomIndex != -1 && topIndex != -1) {
                if (topIndexPrev == -1) {
                    topIndexPrev = topIndex;
                    bottomIndexPrev = bottomIndex;
                    // Add coordinates between bottomIndex and topIndex to 'side1' coordinates list
                    for (int k = bottomIndex; k < topIndex + 1; k++) {
                        addLonLat2List(side1, latDataValuesFlat[k][col], lonDataValuesFlat[k][col], latAttMap,
                         lonAttMap, is360, removeOrigin);
                    }
                } else {
                    topIndexPrev = topIndex;
                    bottomIndexPrev = bottomIndex;
                }
            } else if (bottomIndex == -1 && bottomIndexPrev > -1) {
                // Add coordinates between bottomIndex and topIndex to 'side2' coordinates list
                for (int k = bottomIndexPrev; k < topIndexPrev + 1; k++) {
                    addLonLat2List(side2, latDataValuesFlat[k][col - 1], lonDataValuesFlat[k][col - 1], latAttMap,
                     lonAttMap, is360, removeOrigin);
                    bottomIndexPrev = -1;
                }
            }
        }
    }
    
    /**
     * Apply lat and lon value to coordinate list, apply scale+offset, and
     * convert to ->360 to -180->180, if necessary.
     */
    private void addLonLat2List(List<Coordinate> lonLats, float latValue, float lonValue, Map<String, Double> latAttMap,
                                Map<String, Double> lonAttMap, boolean is360, boolean removeOrigin) {
        if (removeOrigin && latValue == 0d && lonValue == 0) {
            log.trace("Removing Origin");
            return;
        }
        
        double lat = latValue * latAttMap.get("scale") + latAttMap.get("offset");
        double lon = lonValue * lonAttMap.get("scale") + lonAttMap.get("offset");
        
        if (is360 && lon > 180) {
            lon = lon - 360;
        }
        
        //Sanity check. Remove anything outside -180/180 and -90/90
        if (Math.abs(lat) > 90 || Math.abs(lon) > 180) {
            return;
        }
        lonLats.add(new Coordinate(lon, lat));
    }
}
