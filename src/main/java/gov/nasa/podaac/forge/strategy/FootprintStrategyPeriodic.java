package gov.nasa.podaac.forge.strategy;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import gov.nasa.podaac.forge.FootprintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Footprint strategy to be used by the {@link gov.nasa.podaac.forge.Footprinter} which specifies how the footprint
 * is generated.
 * <p>
 * In this case, split on the -180:180 transition (with a bit of a fudge factor of .2)
 */
public class FootprintStrategyPeriodic extends FootprintStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(FootprintStrategyPeriodic.class);
    private final List<List<Coordinate>> coordinateList = new ArrayList<>();
    
    /**
     * Because the top coords are split, update sides 1&2 and the top list with new
     * Coordinate values, creating a smooth transition between the top and sides
     *
     * @param tops   Split list of coordinates (with size > 1)
     * @param sides1 List of coordinates for side1 (any size)
     * @param sides2 List of coordinates for side2 (any size)
     */
    public void processTopCap(List<List<Coordinate>> tops,
                              List<List<Coordinate>> sides1, List<List<Coordinate>> sides2) {
        // If the end of 'top' matches the end of 'side1', extend side1 with the reverse of top,
        // and extend side2 with the second row of top
        if (tops.get(0).get(tops.get(0).size() - 1).x == sides1.get(0).get(sides1.get(0).size() - 1).x) {
            Collections.reverse(tops.get(0));
            sides1.get(0).addAll(tops.get(0));
            closePolygon(sides1.get(0));
            coordinateList.add(sides1.get(0));
            sides1.remove(0);
            sides2.get(0).addAll(0, tops.get(1));
            // If the end of the second row of 'top' matches the end of 'side2', extend sides2 with the second row of
            // 'top',
            // and extend side1 with the first row of top.
        } else if (tops.get(1).get(0).x == sides2.get(0).get(sides2.get(0).size() - 1).x) {
            sides2.get(0).addAll(tops.get(1));
            coordinateList.add(sides2.get(0));
            
            sides2.remove(0);
            List<Coordinate> t = new ArrayList<>(tops.get(0));
            Collections.reverse(t);
            sides1.get(0).addAll(0, t);
        } else {
            // Probably wraps
            log.trace("Odd split...");
            Coordinate topCoordinate = tops.get(0).get(tops.get(0).size() - 1);
            log.trace("oddsplit - x/y:" + topCoordinate.x + "," + topCoordinate.y);
            
            int latSign = topCoordinate.y > 0 ? 1 : -1;
            
            List<Coordinate> newTopCoords = Arrays.asList(new Coordinate(180, latSign * 90),
                    new Coordinate(90, latSign * 90),
                    new Coordinate(0, latSign * 90),
                    new Coordinate(-90, latSign * 90),
                    new Coordinate(-180, latSign * 90));
            // Add new coords to tops from 180 to -180
            if (topCoordinate.x > 0) {
                tops.get(0).addAll(newTopCoords);
                tops.get(0).addAll(tops.get(1));
                // Add new coords to tops from -180 to 180
            } else if (topCoordinate.x < 0) {
                Collections.reverse(newTopCoords);
                tops.get(0).addAll(newTopCoords);
                tops.get(0).addAll(tops.get(1));
            }
            Collections.reverse(tops.get(0));
            sides1.get(0).addAll(0, tops.get(0));
        }
    }
    
    /**
     * Because the bottom coords are split, update sides 1&2 and the bottom list with new
     * Coordinate values, creating a smooth transition between the bottom and sides
     *
     * @param bottoms Split list of coordinates (with size > 1)
     * @param sides1  List of coordinates for side1 (any size)
     * @param sides2  List of coordinates for side2 (any size)
     */
    public void processBottomCap(List<List<Coordinate>> bottoms,
                                 List<List<Coordinate>> sides1, List<List<Coordinate>> sides2) {
        //determine which bottom is the polygon
        if (sides1.get(sides1.size() - 1).get(0).x == bottoms.get(0).get(bottoms.get(0).size() - 1).x) {
            sides1.get(sides1.size() - 1).addAll(bottoms.get(0));
            closePolygon(sides1.get(sides1.size() - 1));
            coordinateList.add(sides1.get(sides1.size() - 1));
            sides1.remove(sides1.size() - 1);
            
            Collections.reverse(bottoms.get(1));
            sides2.get(sides2.size() - 1).addAll(bottoms.get(1));
            
        } else if (sides2.get(sides2.size() - 1).get(0).x == bottoms.get(1).get(0).x) {
            Collections.reverse(sides2.get(sides2.size() - 1));
            bottoms.get(1).addAll(sides2.get(sides2.size() - 1));
            closePolygon(bottoms.get(1));
            coordinateList.add(bottoms.get(1));
            sides2.remove(sides2.size() - 1);
            
            //add remaining bottom to the last of side1
            sides1.get(sides1.size() - 1).addAll(bottoms.get(0));
            
        } else {
            //probably wraps around a bunch of stuff...
            log.trace("Odd split...");
            Coordinate bottomCoordinate = bottoms.get(0).get(bottoms.get(0).size() - 1);
            log.trace("oddsplit - x/y:" + bottomCoordinate.x + "," + bottomCoordinate.y);
            
            int latSign = bottomCoordinate.y > 0 ? 1 : -1;
            
            List<Coordinate> newBottomCoords = Arrays.asList(new Coordinate(180, latSign * 90),
                    new Coordinate(-180, latSign * 90));
            
            if (bottomCoordinate.x > 0) { //180
                //90, 180
                bottoms.get(0).addAll(newBottomCoords);
                bottoms.get(0).addAll(bottoms.get(1));
            } else if (bottomCoordinate.x < 0) { //-180
                Collections.reverse(newBottomCoords);
                bottoms.get(0).addAll(newBottomCoords);
                bottoms.get(0).addAll(bottoms.get(1));
            }
            
            Collections.reverse(bottoms.get(0));
            sides2.get(sides2.size() - 1).addAll(bottoms.get(0));
        }
    }
    
    /**
     * Determine if two coordinates longitude values have different signs.
     *
     * @return True if the two coordinate longitude values have different signs.
     */
    private boolean signChange(Coordinate c1, Coordinate c2) {
        return Math.abs(c1.x - c2.x) > 350;
    }
    
    /**
     * This method takes a top or bottom cap, and the two points that attach to it, to see if there is an issue
     *
     * @param caps            Either the top or bottom cap
     * @param attachedToFirst The coordinate value attached to the first value in the top or bottom cap
     * @param attachedToLast  The coordinate value attached to the last value in the top or bottom cap
     */
    private void fixCaps(List<List<Coordinate>> caps, Coordinate attachedToFirst,
                         Coordinate attachedToLast) {
        List<Coordinate> coordinates = caps.get(0);
        
        // Check to see if attachedToFirst has sign change from first
        if (signChange(coordinates.get(0), attachedToFirst)) {
            log.trace("attachedToFirst Fix");
            List<Coordinate> adds = new ArrayList<>();
            adds.add(new Coordinate((coordinates.get(0).x * -1), coordinates.get(0).y));
            caps.add(0, adds);
        }
        
        // Check to see if attachedToSecond has sign change from last
        if (signChange(coordinates.get(coordinates.size() - 1), attachedToLast)) {
            log.trace("attachedToLast Fix");
            List<Coordinate> adds = new ArrayList<>();
            adds.add(new Coordinate((coordinates.get(coordinates.size() - 1).x * -1),
             coordinates.get(coordinates.size() - 1).y));
            caps.add(caps.size(), adds);
        }
        
    }
    
    /**
     * Merge top, bottom, and sides coordinate lists into a single list of coordinates.
     */
    @Override
    public List<List<Coordinate>> merge(List<Coordinate> side1,
                                        List<Coordinate> bottom, List<Coordinate> side2,
                                        List<Coordinate> top) throws FootprintException {
        
        List<List<Coordinate>> sides1 = split(side1);
        List<List<Coordinate>> sides2 = split(side2);
        List<List<Coordinate>> tops = split(top);
        List<List<Coordinate>> bottoms = split(bottom);
        
        // If tops is empty, try to recalculate using values from sides1 and sides2
        if (tops.size() == 0) {
            //check ends
            List<Coordinate> coords = new ArrayList<>();
            coords.add(sides1.get(0).get(0));
            coords.add(sides2.get(0).get(0));
            tops = split(coords);
            if (tops.size() == 0) {
                throw new FootprintException("No valid values found for 'top-most' row of swath.");
            }
            
            // If there is one coordinate list in tops, but the ends are outside the margin, attempt to fix tops
            // if there's a sign change.
        } else if (tops.size() == 1 && capOutsideMargin(tops.get(0))) {
            fixCaps(tops, sides1.get(0).get(0), sides2.get(0).get(0));
        }
        
        if (bottoms.size() == 0) {
            List<Coordinate> coords = new ArrayList<>();
            
            List<Coordinate> last = sides1.get(sides1.size() - 1);
            coords.add(last.get(last.size() - 1));
            
            
            last = sides2.get(sides2.size() - 1);
            coords.add(last.get(last.size() - 1));
            
            bottoms = split(coords);
            if (bottoms.size() == 0) {
                throw new FootprintException("No valid values found for 'bottom-most' row of swath.");
            }
            
        } else if (bottoms.size() == 1 && capOutsideMargin(bottoms.get(0))) {
            List<Coordinate> s1Last = sides1.get(sides1.size() - 1);
            List<Coordinate> s2Last = sides2.get(sides2.size() - 1);
            fixCaps(bottoms, s1Last.get(s1Last.size() - 1), s2Last.get(s2Last.size() - 1));
        }
        
        if (tops.size() > 1 && bottoms.size() > 1) {
            if (sides1.size() == 1 && sides2.size() == 1) {
                log.trace("Each side is its own polygon");
                Collections.reverse(tops.get(0));
                sides1.get(0).addAll(bottoms.get(0));
                sides1.get(0).addAll(tops.get(0));
                closePolygon(sides1.get(0));
                coordinateList.add(sides1.get(0));
                sides1.remove(0);
                
                tops.get(1).addAll(sides2.get(0));
                Collections.reverse(bottoms.get(1));
                tops.get(1).addAll(bottoms.get(1));
                closePolygon(tops.get(1));
                coordinateList.add(tops.get(1));
                sides2.remove(0);
                return coordinateList;
            }
        }
        if (tops.size() > 1) {
            log.trace("Tops split. Processing.");
            processTopCap(tops, sides1, sides2);
        } else {
            if (sides2.size() != 0) {
                sides2.get(0).addAll(0, tops.get(0));
            } else {
                //need to add the top to... side2
                Collections.reverse(tops.get(0));
                sides1.get(0).addAll(0, tops.get(0));
            }
        }
        
        if (bottoms.size() > 1) {
            log.trace("Bottoms split. Processing.");
            processBottomCap(bottoms, sides1, sides2);
        } else {
            if (sides1.size() != 0) {
                sides1.get(sides1.size() - 1).addAll(bottoms.get(0));
            } else {
                //need to add the bottom to side2
                Collections.reverse(bottoms.get(0));
                sides2.get(sides2.size() - 1).addAll(bottoms.get(0));
            }
        }
        
        if (sides1.size() == 0 && sides2.size() == 0) {
            log.trace("No sides. Returning");
            return coordinateList;
        }
        
        // Make sure merge all the sides without leaving some separated footprint loops
        while (sides1.size() > sides2.size() || sides2.size() > sides1.size()) {
            if (sides1.size() > sides2.size() && sides2.size() != 0) {
                sides1.remove(1);
            } else if (sides2.size() > sides1.size() && sides1.size() != 0) {
                sides2.remove(1);
            } else {
                break;
            }
        }
        
        // Combine sides
        for (int i = 0; i < sides1.size(); i++) {
            log.trace("iteration " + i);
            List<Coordinate> s1 = new ArrayList<>(sides1.get(i));
            if (sides2.size() > i) {
                List<Coordinate> s2 = new ArrayList<>(sides2.get(i));
                Collections.reverse(s2);
                s1.addAll(s2);
            }
            closePolygon(s1);
            coordinateList.add(s1);
        }
        return coordinateList;
    }
    
    /**
     * Close polygon by adding the first point to the end of the list.
     *
     * @param coordinateList The coordinate list to close
     */
    private void closePolygon(List<Coordinate> coordinateList) {
        if (!coordinateList.get(0).equals(coordinateList.get(coordinateList.size() - 1))) {
            coordinateList.add(coordinateList.get(0));
        }
    }
    
    /**
     * Do additional processing in the special case where a polygon intersects both the
     * meridian and antimeridian. Normalize before continuing.
     *
     * @param geometry Geometry to possibly normalize
     * @return Geometry the normalized or unchanged geometry
     */
    public Geometry validate(Geometry geometry) {
        if (geometry instanceof Polygon) {
            GeometryFactory geometryFactory = new GeometryFactory();
            
            Coordinate[] coords = {new Coordinate(0, 90), new Coordinate(0, -90)};
            LineString antimeridian = geometryFactory.createLineString(coords);
            coords[0].x = 180;
            coords[1].x = 180;
            LineString meridian = geometryFactory.createLineString(coords);
    
            if (geometry.intersection(antimeridian).getLength() > 0.0 &&
                geometry.intersection(meridian).getLength() > 0.0) {
                geometry.normalize();
            }
            
            return geometry;
        }
        return geometry;
    }
    
    /**
     * Using the merged list of coordinates representing each side of the data (sides, top, bottom), generate a polygon.
     *
     * @param coords    The merged list of coords
     * @param tolerance Tolerance to use in the simplifier algorithm.
     * @return Footprint geometry
     */
    @Override
    public Geometry mergeGeoms(List<List<Coordinate>> coords, double tolerance) throws FootprintException {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(1000d));
        List<Geometry> geometries = new ArrayList<>();

        double original_tolerance = tolerance;

        for (List<Coordinate> lc : coords) {

            tolerance = original_tolerance;

            LinearRing lr;

            try {
                lr = geometryFactory.createLinearRing(lc.toArray(new Coordinate[0]));
            }
            catch(IllegalArgumentException ex){
                log.error("Error in FootprintStrategyPeriodic while trying to createLinearRing: %s", ex);
                continue;
            }
            
            Polygon polygon = geometryFactory.createPolygon(lr);
            Geometry geometry = DouglasPeuckerSimplifier.simplify(polygon, tolerance);

            geometry = validate(geometry);

            if (!geometry.toText().contains("EMPTY")) {
                
                if (geometry instanceof MultiPolygon) {
                    MultiPolygon multiPolygon = (MultiPolygon) geometry;
                    for (int i = 0; i < multiPolygon.getNumGeometries(); i += 1) {
                        if (multiPolygon.getGeometryN(i).getNumPoints() > 0) {
                            try {
                                geometry = injectPoints((Polygon) multiPolygon.getGeometryN(i));
                            } catch (IllegalArgumentException exception) {
                                log.error("Error while injecting points into polygon: %s", exception);
                                throw new FootprintException("Could not parse expected Polygon. Check for gaps or " +
                                        "invalid/NaN data in data files.");
                            }
                            geometries.add(geometry);
                        }
                    }
                } else {
                    try {
                        geometry = injectPoints((Polygon) geometry);
                    } catch (IllegalArgumentException exception) {
                        log.error("Error while injecting points into polygon: %s", exception);
                        throw new FootprintException("Could not parse expected Polygon. Check for gaps or " +
                                "invalid/NaN data in data files.");
                    }
                    geometries.add(geometry);
                }
            }
            else{
                while(tolerance > 0) {
                    tolerance = tolerance * 0.9;
                    geometry = DouglasPeuckerSimplifier.simplify(polygon, tolerance);
                    if (!geometry.toText().contains("EMPTY")){
                        geometries.add(geometry);
                        break;
                    }
                    if(tolerance <= 0.05 ) {
                        geometries.add(polygon);
                        break;
                    }
                }
            }
        }

        if (geometries.size() > 1) {
            Geometry unionized =  geometryFactory.createMultiPolygon(geometries.toArray(new Polygon[0])).union();
            Geometry simplified = DouglasPeuckerSimplifier.simplify(unionized, original_tolerance);

            //if simplified polygon is empty then return unionized multi polygon
            if (!simplified.toText().contains("EMPTY")){
                return simplified;
            }

            return unionized;
        }
        return geometries.get(0);
    }
}
