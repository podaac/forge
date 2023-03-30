package gov.nasa.podaac.forge.strategy;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import gov.nasa.podaac.forge.FootprintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Footprint strategy to be used by the {@link gov.nasa.podaac.forge.Footprinter} which specifies how the footprint
 * is generated.
 * <p>
 * In this case, split on the -180:180 transition (with a bit of a fudge factor of .2)
 */
public class FootprintStrategyPolarSidesOnly extends FootprintStrategyPolar {
    
    private static final Logger log = LoggerFactory.getLogger(FootprintStrategyPolarSidesOnly.class);
    private final List<List<Coordinate>> coordinateList = new ArrayList<>();
    
    /**
     * When a hole has been found, merge lines to create a flat list of coordinates.
     */
    public void addCoordsFromHole(GeometryFactory geometryFactory, List<List<Coordinate>> sides) throws FootprintException {
        LineString lineString = geometryFactory.createLineString(sides.get(0).toArray(new Coordinate[0]));
        if (!lineString.isSimple()) {
            List<List<Coordinate>> coordinateLists = split(sides.get(0), sides.get(0).size() / 2);
            LineString lineString1 =
             geometryFactory.createLineString(coordinateLists.get(0).toArray(new Coordinate[0]));
            LineString lineString2 =
             geometryFactory.createLineString(coordinateLists.get(1).toArray(new Coordinate[0]));
            Point point;
            try {
                point = (Point) lineString1.intersection(lineString2);
            } catch (java.lang.ClassCastException | com.vividsolutions.jts.geom.TopologyException e) {
                // No intersection of lines so add sides to coordinate list
                sides.get(0).add(sides.get(0).get(0));
                coordinateList.add(sides.get(0));
                return;
            }
            
            log.trace("Intersect at " + point);
            Collections.reverse(coordinateLists.get(0));
            Collections.reverse(coordinateLists.get(1));
            List<Coordinate> merged = mergeLines(coordinateLists.get(0), coordinateLists.get(1), point);
            merged.add(merged.get(merged.size() - 1));
            merged.add(merged.get(0));
            
            coordinateList.add(merged);
        } else {
            sides.get(0).add(sides.get(0).get(0));
            coordinateList.add(sides.get(0));
        }
    }
    
    /**
     * Using the merged list of coordinates representing each side of the data (sides, top, bottom), generate a Polygon
     *
     * @param coords    The merged list of coords
     * @param tolerance Tolerance to use in the simplifier algorithm.
     * @return Footprint geometry (Polygon)
     */
    @Override
    public Geometry mergeGeoms(List<List<Coordinate>> coords, double tolerance) {
        log.trace("coords: " + coords.size());
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(1000d));
        
        List<List<Coordinate>> removes = new ArrayList<>();
        List<List<Coordinate>> slices = new ArrayList<>();
        
        for (List<Coordinate> coordList : coords) {
            if (!coordList.get(0).equals(coordList.get(coordList.size() - 1))) {
                removes.add(coordList);
                // Same longitude at beginning/end
                if (coordList.get(0).x == coordList.get(coordList.size() - 1).x) {
                    log.debug("adding slice " + coordList.get(0).x);
                    slices.add(coordList);
                }
            }
            // Otherwise, first intersects last, so this represents a hole
        }
        coords.removeAll(removes);
        removes.removeAll(slices);
        slices.addAll(processRemoves(removes));
        
        List<Coordinate> globalBox = constructGlobalBox(slices);
        
        globalBox.toArray(new Coordinate[0]);
        LinearRing glr = geometryFactory.createLinearRing(globalBox.toArray(new Coordinate[0]));
        LinearRing[] holes = new LinearRing[coords.size()];
        
        int index = 0;
        for (List<Coordinate> coord : coords) {
            LinearRing linearRing = geometryFactory.createLinearRing(coord.toArray(new Coordinate[0]));
            linearRing = (LinearRing) DouglasPeuckerSimplifier.simplify(linearRing, tolerance);
            holes[index++] = linearRing;
        }

        Polygon polygon = geometryFactory.createPolygon(glr, holes);
        try {
            polygon = (Polygon) DouglasPeuckerSimplifier.simplify(polygon, tolerance);
            polygon = (Polygon) injectPoints(polygon);
        }

        catch(ClassCastException exc){
            // can't cast a multipolygon to polygon so return a multipolygon
            log.debug("Exception during mergeGeoms StrategyPolarSidesOnly " + exc);
            return DouglasPeuckerSimplifier.simplify(polygon, tolerance);
        }
        
        return polygon;
    }
    
    private Collection<? extends List<Coordinate>> processRemoves(List<List<Coordinate>> removes) {
        List<List<Coordinate>> rets = new ArrayList<>();
        log.trace("Remove size: " + removes.size());
        if (removes.size() == 2) {
            
            List<Coordinate> coordinateList1 = removes.get(0);
            List<Coordinate> coordinateList2 = removes.get(1);
            
            List<Coordinate> start = null;
            List<Coordinate> end = null;
            
            Coordinate coordinate1 = coordinateList1.get(0);
            Coordinate coordinate2 = coordinateList2.get(0);
            
            log.trace("coordinateList1" + coordinate1.x);
            if (coordinate1.x == 180d || coordinate1.x == -180d) {
                start = coordinateList1;
            } else {
                end = coordinateList1;
            }
            
            log.trace("coordinateList2" + coordinate2.x);
            if (coordinate2.x == 180d || coordinate2.x == -180d) {
                start = coordinateList2;
            } else {
                end = coordinateList2;
            }
            
            if (start != null && end != null) {
                if (start.get(0).x == end.get(end.size() - 1).x) {
                    List<Coordinate> mergedList = mergeLists(start, end);
                    if (mergedList == null) {
                        start.addAll(end);
                    } else {
                        start.addAll(mergedList);
                    }
                    rets.add(start);
                } else {
                    log.trace("The first and last points in the start and end coordinate lists do not match.");
                    start.add(new Coordinate(start.get(0).x, start.get(start.size() - 1).y));
                    Coordinate add = new Coordinate(end.get(end.size() - 1).x, end.get(0).y);
                    end.add(0, add);
                    rets.add(start);
                    rets.add(end);
                }
            } else {
                log.trace("None!");
            }
            
        }
        return rets;
    }
    
    /**
     * Merge sides/top/bottom lists of coordinates into a single list of lists of coordinates.
     */
    @Override
    public List<List<Coordinate>> merge(List<Coordinate> side1,
                                        List<Coordinate> bottom,
                                        List<Coordinate> side2,
                                        List<Coordinate> top) throws FootprintException {
        
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(1000d));

        List<List<Coordinate>> sides1;
        List<List<Coordinate>> sides2;

        if(top.size() > side1.size()){
            sides1 = split(top);
        }
        else{
            sides1 = split(side1);
        }

        if(bottom.size() > side2.size()){
            sides2 = split(bottom);
        }
        else{
            sides2 = split(side2);
        }

        // Start with a rectangle covering the entire globe
        
        if (sides1.size() == 1) {
            // Hole
            addCoordsFromHole(geometryFactory, sides1);
        } else {
            coordinateList.addAll(sides1);
        }
        
        if (sides2.size() == 1) {
            // Hole
            addCoordsFromHole(geometryFactory, sides2);
        } else {
            coordinateList.addAll(sides2);
        }
        return coordinateList;
    }
    
    /**
     * Merge provided coordinate lists by finding the points closest to the provided point, and merging the beginning
     * of the first list and the end of the second list at that point.
     *
     * @return The merged coordinate list
     */
    private List<Coordinate> mergeLines(List<Coordinate> start, List<Coordinate> end, Point point) {
        Coordinate coord = point.getCoordinate();
        return mergeLines(start, end, coord);
    }
}
