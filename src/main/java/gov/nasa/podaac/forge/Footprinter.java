package gov.nasa.podaac.forge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import gov.nasa.podaac.forge.pojo.DatasetConfig;
import gov.nasa.podaac.forge.pojo.FootprintConfig;
import gov.nasa.podaac.forge.strategy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used for footprinting a granule.
 */
public class Footprinter {
    
    private static final Logger log = LoggerFactory.getLogger(Footprinter.class);
    private static final String EXTENT = "EXTENT";
    private static final String FOOTPRINT = "FOOTPRINT";
    private static final String FILL = "fill";
    private static final String SCALE = "scale";
    private static final String OFFSET = "offset";
    
    private final Gson gson;
    private DatasetConfig datasetConfig;
    private String granuleFile;
    
    public Footprinter(String granuleFile, String configFile) throws IOException {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.granuleFile = granuleFile;
        this.datasetConfig = parseConfig(configFile);
    }
    
    Footprinter() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Given a dataset config filename, generate a DatasetConfig object containing the relevant configuration values
     * for the footprint operation.
     *
     * @param configFileLocation The file path of the dataset configuration file
     * @return A DatasetConfig object which contains the configuration values from the config file.
     * @throws FileNotFoundException If the given dataset config file is not found.
     */
    public DatasetConfig parseConfig(String configFileLocation) throws FileNotFoundException {
        DatasetConfig datasetConfig = gson.fromJson(new FileReader(configFileLocation), DatasetConfig.class);
        
        if (datasetConfig.getLonVar() == null || datasetConfig.getLatVar() == null) {
            throw new JsonParseException("'latVar' and 'lonVar' must be provided in the dataset config");
        }
        return datasetConfig;
    }
    
    /**
     * Given a pattern like A:B,C:D,E:F,etc..., construct a list of NetCDF4 ranges which will be used to generate the
     * footprint for this granule.
     *
     * @param pattern A string pattern which represents what type of ranges to return, and in what order.
     * @param shapes  The size of the data, used when constructing the ranges.
     * @return A list of ranges, constructed from the given pattern and data shape.
     * @throws InvalidRangeException The function assumes the form A:B,C:D,etc. If the correct format is not provided,
     *                               this error may occur.
     */
    public List<Range> buildRanges(String pattern, int[] shapes) throws InvalidRangeException {
        String[] splits = pattern.split(",");
        List<Range> ranges = new ArrayList<>();
        for (int i = 0; i < splits.length; i++) {
            int range1, range2;
            String[] splitPattern = splits[i].split(":");
            
            if (splitPattern[0].equals("*")) {
                range1 = shapes[i] - 1;
            } else {
                range1 = Integer.parseInt(splitPattern[0]);
            }
            
            if (splitPattern[1].equals("*")) {
                range2 = shapes[i] - 1;
            } else {
                range2 = Integer.parseInt(splitPattern[1]);
            }
            ranges.add(new Range(range1, range2));
        }
        
        return ranges;
    }

    // Helper method to determine if longitude values are in 360-degree format
    private boolean isLongitude360(double[] lonValues) {
        for (double lon : lonValues) {
            if (!Double.isNaN(lon) && lon > 180.0) {
                return true;
            }
        }
        return false;
    }

    /**
    * Checks whether the given latitude and longitude variables contain at least one valid coordinate pair.
    *
    * <p>The method reads the values from the provided latitude and longitude variables, applies
    * scale and offset corrections from the corresponding attribute maps, and verifies if at least
    * one coordinate pair falls within the valid geographic bounds.</p>
    *
    * @param latVariable  The latitude variable containing raw latitude values.
    * @param lonVariable  The longitude variable containing raw longitude values.
    * @param latAttMap    A map containing the scale and offset attributes for latitude correction.
    * @param lonAttMap    A map containing the scale and offset attributes for longitude correction.
    * @param is360        A flag indicating whether the longitude range is [0, 360] (true) or [-180, 180] (false).
    * @return             {@code true} if at least one valid coordinate pair is found, otherwise {@code false}.
    * @throws IOException If reading from the variables fails.
    */
    public boolean hasValidCoordinatePair(Variable latVariable, Variable lonVariable, 
                                        Map<String, Double> latAttMap, Map<String, Double> lonAttMap) throws IOException {
        Array latValues = latVariable.read();
        Array lonValues = lonVariable.read();
        
        final String SCALE = "scale";
        final String OFFSET = "offset";
        
        int size = (int) Math.min(latValues.getSize(), lonValues.getSize());
        
        // Create arrays to store transformed values
        double[] transformedLats = new double[size];
        double[] transformedLons = new double[size];
        
        // First pass: Apply scale and offset to all values
        for (int i = 0; i < size; i++) {
            transformedLats[i] = latValues.getDouble(i) * latAttMap.get(SCALE) + latAttMap.get(OFFSET);
            transformedLons[i] = lonValues.getDouble(i) * lonAttMap.get(SCALE) + lonAttMap.get(OFFSET);
        }
        
        // Determine if longitude is 360 based on transformed values
        boolean is360 = isLongitude360(transformedLons);
        
        // Constants for validation
        final double MIN_LAT = -90.0;
        final double MAX_LAT = 90.0;
        final double MIN_LON = is360 ? 0.0 : -180.0;
        final double MAX_LON = is360 ? 360.0 : 180.0;
        
        // Second pass: Check for valid pairs using transformed values
        for (int i = 0; i < size; i++) {
            double lat = transformedLats[i];
            double lon = transformedLons[i];
            
            if (!Double.isNaN(lat) && !Double.isNaN(lon) &&
                lat >= MIN_LAT && lat <= MAX_LAT &&
                lon >= MIN_LON && lon <= MAX_LON) {
                return true;  // Found at least one valid pair
            }
        }
        
        return false;
    }

    /**
     * Do the work of footprinting the NetCDF4 file.
     *
     * @return Map containing the result. This map contains the keys 'EXTENT' and 'FOOTPRINT', where 'EXTENT'
     * represents the WKT spatial bounds (a.k.a bbox) and the 'FOOTPRINT' represents the WKT footprint (might be a
     * POLYGON, LINESTRING, ...)
     */
    public Map<String, String> footprint() throws FootprintException, InvalidRangeException {
        Map<String, String> footprintMap = new HashMap<>();
        
        FootprintConfig footprint = datasetConfig.getFootprint();
        FootprintStrategy.Strategy footprintStrategyType = datasetConfig.getFootprint().getStrategy();
        FootprintStrategy footprintStrategy;
        
        switch (footprintStrategyType) {
            case PERIODIC:
                footprintStrategy = new FootprintStrategyPeriodic();
                break;
            case LINE_STRING:
                footprintStrategy = new FootprintStrategyLinestring();
                break;
            case POLAR:
                footprintStrategy = new FootprintStrategyPolar();
                break;
            case POLAR_SIDES:
                footprintStrategy = new FootprintStrategyPolarSidesOnly();
                break;
            case SMAP:
                footprintStrategy = new FootprintStrategyPolarSmap();
                break;
            case SWOT_LINESTRING:
                footprintStrategy = new FootprintStrategyLinestring();
                break;
            default:
                log.error("The provided footprint strategy {} is invalid", footprintStrategyType.getStrategyName());
                throw new FootprintException("Footprint strategy " + footprintStrategyType.getStrategyName() + " was not recognized");
        }
        
        boolean is360 = datasetConfig.isIs360();
        boolean findValid = footprint.isFindValid();
        boolean removeOrigin = footprint.isRemoveOrigin();
        List<Coordinate> top = new ArrayList<>();
        List<Coordinate> bottom = new ArrayList<>();
        List<Coordinate> side1 = new ArrayList<>();
        List<Coordinate> side2 = new ArrayList<>();
        List<Range> rangeList;
        
        try (NetcdfFile dataFile = NetcdfFile.open(granuleFile, null)) {
            Variable lonVariable = dataFile.findVariable(datasetConfig.getLonVar());
            Variable latVariable = dataFile.findVariable(datasetConfig.getLatVar());
            Map<String, Double> latAttMap = getAttributes(latVariable);
            Map<String, Double> lonAttMap = getAttributes(lonVariable);
            int[] shapes = latVariable.getShape();

            boolean isValidLonLat = hasValidCoordinatePair(latVariable, lonVariable, latAttMap, latAttMap);

            if(!isValidLonLat){
                throw new FootprintException("The granule trying to footprint doesn't have any valid longitude and latitude data.");
            }

            String strategyName = footprintStrategyType.getStrategyName();
            if(strategyName == "swot_linestring"){
                rangeList = buildRanges(footprint.getSide1(), shapes);
                side1 = processRange(rangeList, lonVariable, latVariable, lonAttMap, latAttMap, strategyName);      
                top = null;
                bottom = null;
                side2 = null;
            }

            else{
                if (footprint.getSide1() != null) {
                    rangeList = buildRanges(footprint.getSide1(), shapes);
                    side1 = processRange(rangeList, lonVariable, latVariable, lonAttMap, latAttMap, strategyName);
                    
                }
                
                if (footprint.getBottom() != null) {
                    rangeList = buildRanges(footprint.getBottom(), shapes);
                    bottom = processRange(rangeList, lonVariable, latVariable, lonAttMap, latAttMap, strategyName);
                    
                }
                if (footprint.getSide2() != null) {
                    rangeList = buildRanges(footprint.getSide2(), shapes);
                    side2 = processRange(rangeList, lonVariable, latVariable, lonAttMap, latAttMap, strategyName);
                    
                }
                
                if (footprint.getTop() != null) {
                    rangeList = buildRanges(footprint.getTop(), shapes);
                    top = processRange(rangeList, lonVariable, latVariable, lonAttMap, latAttMap, strategyName);
                    
                }
            }
            
            if (footprintStrategyType == FootprintStrategy.Strategy.SMAP) {
                footprintStrategy.calculateFootprint(lonVariable, latVariable, latAttMap, lonAttMap, side1, side2, top,
                        bottom, is360, findValid, removeOrigin);
            }
            
        } catch (IOException exception) {
            log.error("Unable to open NetCDF file {}", granuleFile);
            throw new FootprintException("Error while opening granule file", exception);
        }
        
        List<List<Coordinate>> coords = footprintStrategy.merge(side1, bottom, side2, top);
        Geometry geometry = footprintStrategy.mergeGeoms(coords, datasetConfig.getTolerance());
        
        footprintMap.put(FOOTPRINT, geometry.toText());
        footprintMap.put(EXTENT, geometry.getEnvelope().toText());
        return footprintMap;
    }
    
    /**
     * For the given NetCDF4 lat/lon variables, return a list of (X,Y) coordinates. 'fill' values are not part of the
     * result.
     *
     * @param rangeList List of ranges used to retrieve the coordinates from this granule.
     *                  180. This function will adjust the longitude values, so the returned coordinates will be -180
     *                   to 180.
     * @param lonVariable The longitude variable from the granule file
     * @param latVariable The latitude variable from the granule file
     * @param lonAttMap A map containing scale, offset, and fill for the longitude variable
     * @param latAttMap A map containing scale, offset, and fill for the latitude variable
     * @return Constructed list of lat/lon coordinates, masked and scaled using variable attributes.
     */
    public List<Coordinate> constructCoordsFromNetcdf(List<Range> rangeList, Variable lonVariable, Variable latVariable,
                                                      Map<String, Double> lonAttMap, Map<String, Double> latAttMap, String strategyName)
            throws IOException, InvalidRangeException {

        Array latData = null;
        Array lonData = null;
        if(strategyName == "swot_linestring"){
            latData = latVariable.read();
            lonData = lonVariable.read();
        }
        else{
            latData = latVariable.read(rangeList);
            lonData = lonVariable.read(rangeList);
        }

        boolean is360 = datasetConfig.isIs360();
        boolean removeOrigin = datasetConfig.getFootprint().isRemoveOrigin();

        List<Coordinate> lonLats = new ArrayList<>();
        for (int i = 0; i < latData.getSize(); i++) {
            if (latData.getDouble(i) == latAttMap.get(FILL) || lonData.getDouble(i) == lonAttMap.get(FILL)) {
                continue;
            } else if (removeOrigin && latData.getDouble(i) == 0d && lonData.getDouble(i) == 0) {
                continue;
            }
            
            double lat = latData.getDouble(i) * latAttMap.get(SCALE) + latAttMap.get(OFFSET);
            double lon = lonData.getDouble(i) * lonAttMap.get(SCALE) + lonAttMap.get(OFFSET);
            
            if (is360 && lon > 180) {
                lon = lon - 360;
            }
            
            //Sanity check. Remove anything outside -180/180 and -90/90
            if (Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                continue;
            }
            
            lonLats.add(new Coordinate(lon, lat));
        }
        return lonLats;
    }
    
    
    /**
     * Calculate a list of coordinates for the given ranges. The ranges will be adjusted as necessary until valid
     * coordinates are found.
     *
     * @param rangeList List of ranges used to retrieve list of coords
     * @param lonVariable The longitude variable from the granule file
     * @param latVariable The latitude variable from the granule file
     * @param lonAttMap A map containing scale, offset, and fill for the longitude variable
     * @param latAttMap A map containing scale, offset, and fill for the latitude variable
     * @return A list of lon/lat coordinates
     */
    public List<Coordinate> processRange(List<Range> rangeList, Variable lonVariable, Variable latVariable,
                                         Map<String, Double> lonAttMap, Map<String, Double> latAttMap, String strategyName)
            throws IOException, InvalidRangeException {
        
        boolean findValid = datasetConfig.getFootprint().isFindValid();
        Boolean fromZero = null;
        
        // Check if the current range contains any coordinates. If not, move the range to the right or left, and
        // keep retrying until lonLats contains values.
        do {
            List<Coordinate> lonLats = constructCoordsFromNetcdf(rangeList, lonVariable, latVariable, lonAttMap, latAttMap, strategyName);
            // If findValid is false, or lonLats contains values, just return lonLats.
            if (!findValid || !lonLats.isEmpty()) {
                return lonLats;
            }
            // Otherwise, adjust range and try again.
            Range newRange = null;
            int rangeIndex = 0;
            for (int j = 0; j < rangeList.size(); j++) {
                Range range = rangeList.get(j);
                if (range.first() == range.last()) {
                    if (fromZero == null) {
                        fromZero = range.first() == 0;
                    }
                    if (fromZero) {
                        newRange = new Range(range.first() + 1, range.last() + 1);
                    } else {
                        newRange = new Range(range.first() - 1, range.last() - 1);
                    }
                    rangeIndex = j;
                }
            }
            rangeList.set(rangeIndex, newRange);
        } while (true);
    }
    
    /**
     * Get 'scale', 'fill', and 'offset' attributes from the given NetCDF4 variable, and return in a Map.
     *
     * @param var The NetCDF4 variable to retrieve attributes from
     * @return A Map where the keys are 'scale', 'fill', and 'offset'.
     */
    public Map<String, Double> getAttributes(Variable var) {
        Map<String, Double> attributeMap = new HashMap<>();
        
        double scale = 1d;
        double fill = Double.MIN_VALUE;
        double offset = 0d;
        
        for (Attribute attribute : var.getAttributes()) {
            String attributeName = attribute.getShortName().toLowerCase();
            if (attributeName.contains(FILL)) {
                fill = attribute.getNumericValue().doubleValue();
            } else if (attributeName.contains(SCALE)) {
                scale = attribute.getNumericValue().doubleValue();
            } else if (attributeName.contains(OFFSET)) {
                offset = attribute.getNumericValue().doubleValue();
            }
        }
        
        attributeMap.put(SCALE, scale);
        attributeMap.put(FILL, fill);
        attributeMap.put(OFFSET, offset);
        return attributeMap;
    }
}
