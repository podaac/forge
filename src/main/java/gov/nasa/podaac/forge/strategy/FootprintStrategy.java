package gov.nasa.podaac.forge.strategy;

import com.google.gson.annotations.SerializedName;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import gov.nasa.podaac.forge.FootprintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class FootprintStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(FootprintStrategy.class);
    protected double splitMargin = 179.99d;
    
    public enum Strategy {
        @SerializedName("periodic")
        PERIODIC("periodic"),
        @SerializedName("linestring")
        LINE_STRING("linestring"),
        @SerializedName("polar")
        POLAR("polar"),
        @SerializedName("polarsides")
        POLAR_SIDES("polarsides"),
        @SerializedName("smap")
        SMAP("smap"),
        @SerializedName("fixed")
        FIXED("fixed"),
        @SerializedName("swot_linestring")
        SWOT_LINESTRING("swot_linestring");
        
        private final String strategyName;
        
        Strategy(String strategyName) {
            this.strategyName = strategyName;
        }
        
        public String getStrategyName() {
            return this.strategyName;
        }
        
        public static Strategy fromString(String strategyName) {
            return Arrays.stream(Strategy.values())
                    .filter(strategy -> strategy.strategyName.equalsIgnoreCase(strategyName))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        }
    }
    
    public abstract List<List<Coordinate>> merge(List<Coordinate> side1,
                                                 List<Coordinate> bottom, List<Coordinate> side2,
                                                 List<Coordinate> top) throws FootprintException;
    
    public abstract Geometry mergeGeoms(List<List<Coordinate>> coords, double d) throws FootprintException;
    
    public void calculateFootprint(Variable lonVariable, Variable latVariable,
                                   Map<String, Double> latAttMap, Map<String, Double> lonAttMap,
                                   List<Coordinate> side1, List<Coordinate> side2,
                                   List<Coordinate> top, List<Coordinate> bottom,
                                   boolean is360, boolean findValid, boolean removeOrigin) throws IOException,
                                    InvalidRangeException {
    }
    
    public Geometry injectPoints(Polygon geometry) {
        Coordinate[] coords = geometry.getExteriorRing().getCoordinates();
        List<Coordinate> coordinateList = new ArrayList<>();
        
        PrecisionModel precisionModel = new PrecisionModel(10000d);
        GeometryFactory geometryFactory = new GeometryFactory(precisionModel);
        
        for (int i = 0; i < coords.length; i++) {
            if (i == 0) {
                coordinateList.add(coords[i]);
                continue;
            }
            
            if (coords[i].x == 180 && Math.abs(coords[i].y) == 90) {
                if (coords[i - 1].x == -180) {
                    //add points
                    coordinateList.add(new Coordinate(-90, coords[i].y));
                    coordinateList.add(new Coordinate(0, coords[i].y));
                    coordinateList.add(new Coordinate(90, coords[i].y));
                }
            }
            if (coords[i].x == -180 && Math.abs(coords[i].y) == 90) {
                if (coords[i - 1].x == 180) {
                    //add points
                    coordinateList.add(new Coordinate(90, coords[i].y));
                    coordinateList.add(new Coordinate(0, coords[i].y));
                    coordinateList.add(new Coordinate(-90, coords[i].y));
                }
            }
            coordinateList.add(coords[i]);
        }
        
        LinearRing[] holes = new LinearRing[geometry.getNumInteriorRing()];
        for (int i = 0; i < geometry.getNumInteriorRing(); i++) {
            LinearRing lr = geometryFactory.createLinearRing(geometry.getInteriorRingN(i).getCoordinates());
            holes[i] = lr;
        }
        
        coordinateList.toArray(new Coordinate[0]);
        LinearRing linearRing = geometryFactory.createLinearRing(coordinateList.toArray(new Coordinate[0]));
        return geometryFactory.createPolygon(linearRing, holes);
    }
    
    /**
     * Determine if end coords are outside 178 margin
     *
     * @param coords The list of coordinates to check the ends of
     * @return Return true if the first or last coordinate in this list of coordinates longitude value is > 178
     */
    public boolean capOutsideMargin(List<Coordinate> coords) {
        return Math.abs(coords.get(0).x) > 178d || Math.abs(coords.get(coords.size() - 1).x) > 178d;
    }
    
    /**
     * Split a list of coordinate into separate lists of coordinates. A coordinate is split into a separate list if
     * the coordinate differs wildly from the previous coordinate. For example, if one coordinate longitude is -175,
     * and the next coordinate longitude is 175, it can be assumed that those values represent a split.
     *
     * @param coordinateList The list of coordinates to be split into multiple lists of coordinates
     * @return A list of lists of coordinates, where each list represents a split.
     */
    protected List<List<Coordinate>> split(List<Coordinate> coordinateList) {
        List<List<Coordinate>> splitCoordList = new ArrayList<>();
        if (coordinateList == null) {
            log.warn("Unable to split given null coordinate list. Returning empty split.");
            return splitCoordList;
        }
        
        double margin = this.splitMargin;
        Coordinate prevCoord = null;
        Coordinate currentCoord = null;
        
        boolean lastInMargin = false;
        boolean firstInMargin = false;
        List<Coordinate> coords = new ArrayList<>();
        
        for (int i = 0; i < coordinateList.size(); i++) {
            currentCoord = coordinateList.get(i);
            
            // Check if the first point in the list of coordinates is within the margin
            // If so, continue to the next iteration of the loop
            if (Math.abs(currentCoord.x) > margin) {
                if (i == 0)
                    firstInMargin = true;
                lastInMargin = true;
                continue;
            }
            lastInMargin = false;
            
            // If the first point in the list was within the margin, add a new point
            // to the list with lon -180/180
            if (firstInMargin) {
                if (currentCoord.x < 0) {
                    coords.add(new Coordinate(-180, currentCoord.y));
                } else {
                    coords.add(new Coordinate(180, currentCoord.y));
                }
                // Set firstInMargin to false, so this code will only ever execute once (if ever)
                firstInMargin = false;
            }
            
            if (prevCoord == null) {
                prevCoord = currentCoord;
                coords.add(currentCoord);
            } else {
                Coordinate lastCoord = coords.get(coords.size() - 1);
                double yAverage = (lastCoord.y + currentCoord.y) / 2;
                // If two points appear that are wildly different from one another, consider that to be a 'split'.
                // Add the existing coordinates to the list of coordinate lists, and start a new coordinate
                // list for the current split.
                if (prevCoord.x > 150 && currentCoord.x < -150) {
                    log.debug("SPLIT!");
                    // Add a new coord with max lon, and with lat equal to the average between the current
                    // and previous lat.
                    coords.add(new Coordinate(180d, yAverage));
                    splitCoordList.add(coords);
                    coords = new ArrayList<>();
                    // Add a new coord with min lon, and with lat equal to the average between the current
                    // and previous lat.
                    coords.add(new Coordinate(-180d, yAverage));
                } else if (currentCoord.x > 150 && prevCoord.x < -150) {
                    log.debug("SPLIT!");
                    // Add a new coord with min lon, and with lat equal to the average between the current
                    // and previous lat.
                    coords.add(new Coordinate(-180d, yAverage));
                    splitCoordList.add(coords);
                    coords = new ArrayList<>();
                    // Add a new coord with max lon, and with lat equal to the average between the current
                    // and previous lat.
                    coords.add(new Coordinate(180d, yAverage));
                }
                coords.add(currentCoord);
                prevCoord = currentCoord;
            }
        }
        if (lastInMargin) {
            log.trace("Last coordinate was in margin or NaN...");
            if (prevCoord == null) {
                return splitCoordList;
            }
            if (prevCoord.x > 0) {
                coords.add(new Coordinate(180, currentCoord.y));
            } else {
                coords.add(new Coordinate(-180, currentCoord.y));
            }
        }

        // If coords is empty don't return [[]] as later part of the code will attempt to fix the footprint
        // if there is an empty list of coordinates.
        if(coords.size() > 0){
            splitCoordList.add(coords);
        }
        return splitCoordList;
    }
    
    /**
     * Find the index of the element in the given list of coordinates which is closest to the given coordinate.
     *
     * @param coordinates The list of coordinates to find the closest coordinate from.
     * @param coordinate  The coordinate to compare distance against
     * @return the integer index of the closest coordinate.
     */
    public int findClosest(List<Coordinate> coordinates, Coordinate coordinate) {
        double distance = Double.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < coordinates.size(); i++) {
            if (coordinates.get(i).distance(coordinate) < distance) {
                distance = coordinates.get(i).distance(coordinate);
                index = i;
            }
        }
        return index;
    }
    
    /**
     * Split a list of coordinate into two separate lists at the given index, and return a list containing both lists.
     */
    public List<List<Coordinate>> split(List<Coordinate> sides, int index) {
        List<List<Coordinate>> coordinateLists = new ArrayList<>();
        coordinateLists.add(sides.subList(0, index));
        coordinateLists.add(sides.subList(index, sides.size()));
        return coordinateLists;
    }
}
