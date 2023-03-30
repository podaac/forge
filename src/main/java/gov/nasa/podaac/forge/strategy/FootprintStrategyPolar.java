package gov.nasa.podaac.forge.strategy;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import gov.nasa.podaac.forge.FootprintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Footprint strategy to be used by the {@link gov.nasa.podaac.forge.Footprinter} which specifies how the footprint
 * is generated.
 */
public class FootprintStrategyPolar extends FootprintStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(FootprintStrategyPolar.class);
    private final List<List<Coordinate>> coordinateLists = new ArrayList<>();
    
    /**
     * Given a list of lists of coordinates, reverse each list.
     */
    public List<List<Coordinate>> reverseLists(List<List<Coordinate>> coordLists) {
        List<List<Coordinate>> coordListsReverse = new ArrayList<>(coordLists);
        coordListsReverse.forEach(Collections::reverse);
        return coordListsReverse;
    }
    
    /**
     * Merge sides/top/bottom lists of coordinates into a single list of lists of coordinates.
     */
    @Override
    public List<List<Coordinate>> merge(List<Coordinate> side1,
                                        List<Coordinate> bottom,
                                        List<Coordinate> side2,
                                        List<Coordinate> top) throws FootprintException {
        List<List<Coordinate>> sides1 = split(side1);
        List<List<Coordinate>> sides2 = split(side2);
        List<List<Coordinate>> tops = split(top);
        List<List<Coordinate>> bottoms = split(bottom);
        
        // Goal is to create the connected lines that will either create holes in the global rectangle
        // or will away into the -180 or 180 meridians
        
        procSide(sides1, tops, bottoms, "sides1");
        
        //reverse each list in the coordinate lists
        List<List<Coordinate>> topsR = reverseLists(tops);
        List<List<Coordinate>> bottomsR = reverseLists(bottoms);
        List<List<Coordinate>> sides2R = reverseLists(sides2);
        
        procSide(sides2R, bottomsR, topsR, "sides2");
        
        if (coordinateLists.size() <= 1) {
            log.trace("Getting ordered list...");
            List<List<Coordinate>> llc = new ArrayList<>();
            llc.addAll(bottoms);
            llc.addAll(tops);
            
            llc.addAll(sides1);
            llc.addAll(sides2);
            llc.removeAll(coordinateLists);
            
            List<Coordinate> ll = orderLists(llc);
            if (ll != null) {
                coordinateLists.add(ll);
            }
        }
        return coordinateLists;
    }
    
    /**
     * Find rows in the given list of coordinate lists where the beginning and end of each list
     * matches the beginning and end of the first element of the given coordinate list.
     */
    private List<Coordinate> orderLists(List<List<Coordinate>> llc) {
        List<Coordinate> theOne = llc.remove(0);
        return findOne(theOne, llc);
    }
    
    /**
     * Recursively find rows in llc which match the beginning or end of coordList. Augment coordList with those values
     * and repeat until llc is empty.
     */
    public List<Coordinate> findOne(List<Coordinate> coordList, List<List<Coordinate>> llc) {
        
        if (llc.isEmpty()) {
            return coordList;
        }
        
        log.trace("in FindOne!");
        log.trace("theOne0" + coordList.get(0));
        log.trace("theOneN " + coordList.get(coordList.size() - 1));
        
        int index = -1;
        for (int i = 0; i < llc.size(); i++) {
            List<Coordinate> coords = llc.get(i);
            log.trace("testing " + i);
            log.trace("next0 " + coords.get(0));
            log.trace("nextN " + coords.get(coords.size() - 1));
            
            // If the first element of coords matches the last element of coordList
            if (coords.get(0).equals(coordList.get(coordList.size() - 1))) {
                coordList.addAll(coords);
                index = i;
                break;
                // If the last element of coords matches the first element of coordList
            } else if (coords.get(coords.size() - 1).equals(coordList.get(0))) {
                coordList.addAll(0, coords);
                index = i;
                break;
                // If the last element of coords matches the last element of coordList
            } else if (coords.get(coords.size() - 1).equals(coordList.get(coordList.size() - 1))) {
                Collections.reverse(coords);
                coordList.addAll(coords);
                index = i;
                break;
                // If the first element of coords matches the first element of coordList
            } else if (coords.get(0).equals(coordList.get(0))) {
                Collections.reverse(coords);
                coordList.addAll(0, coords);
                index = i;
                break;
            }
        }
        if (index == -1) {
            log.debug("No Match Found!");
            return null;
        }
        llc.remove(index);
        return findOne(coordList, llc);
    }
    
    /**
     * Given side(either 1 or 2)/top/bottom coordinate lists, create connected lines that will create holes in the
     * global rectangle.
     */
    private void procSide(List<List<Coordinate>> sides,
                          List<List<Coordinate>> tops,
                          List<List<Coordinate>> bottoms,
                          String label) {
        
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(1000d));
        
        if (sides.size() == 1) {
            LineString lineString = geometryFactory.createLineString(sides.get(0).toArray(new Coordinate[0]));
            
            // sides is not a 'simple' geometry, and therefore intersects itself.
            // Creating a hole in this geometry.
            if (!lineString.isSimple()) {
                log.trace(label + ", side intersects itself. Creating a hole");
                List<List<Coordinate>> coordinateLists = split(sides.get(0), sides.get(0).size() / 2);
                Coordinate coordinate = intersection(coordinateLists.get(0), coordinateLists.get(1));
                log.trace("Intersect at " + coordinate);
                Collections.reverse(coordinateLists.get(0));
                Collections.reverse(coordinateLists.get(1));
                List<Coordinate> merged = mergeLines(coordinateLists.get(0), coordinateLists.get(1), coordinate);
                merged.add(merged.get(merged.size() - 1));
                merged.add(merged.get(0));
                this.coordinateLists.add(merged);
            }
            // DOES NOT INTERSECT ITSELF
            else {
                log.trace(label + ", size1 DOES NOT intersect itself. May be a hole, may be an edge case");
                // check to see if 'top' or 'bottom' intersects with self.
                // tops first
                for (List<Coordinate> coordList : tops) {
                    LineString rowLineString = geometryFactory.createLineString(coordList.toArray(new Coordinate[0]));
                    MultiPoint multiPoint = (MultiPoint) lineString.intersection(rowLineString);
                    log.trace(String.valueOf(multiPoint));
                    for (Coordinate coordinate : multiPoint.getCoordinates()) {
                        if (!coordinate.equals(sides.get(0).get(0))) {
                            // Found a hole
                            int i = findClosest(sides.get(0), coordinate);
                            sides.set(0, sides.get(0).subList(0, i));
                            sides.get(0).add(sides.get(0).get(0));
                            coordinateLists.add(sides.get(0));
                        }
                    }
                }
                // bottoms
                for (List<Coordinate> coordList : bottoms) {
                    LineString rowLineString = geometryFactory.createLineString(coordList.toArray(new Coordinate[0]));
                    MultiPoint multiPoint = (MultiPoint) lineString.intersection(rowLineString);
                    log.trace(String.valueOf(multiPoint));
                    for (Coordinate coordinate : multiPoint.getCoordinates()) {
                        if (!coordinate.equals(sides.get(0).get(sides.get(0).size() - 1))) {
                            // Found a hole
                            int i = findClosest(sides.get(0), coordinate);
                            sides.set(0, sides.get(0).subList(i, sides.get(0).size()));
                            sides.get(0).add(sides.get(0).get(0));
                            coordinateLists.add(sides.get(0));
                        }
                    }
                }
                
            }
            //Multiple sides, not one ring
        } else {
            log.trace("Sides1, size >1, looking for slices. " + sides.size());
            List<List<Coordinate>> removes = new ArrayList<>();
            for (List<Coordinate> coordList : sides) {
                if (!coordList.get(0).equals(coordList.get(coordList.size() - 1))) {
                    if (coordList.get(0).x == coordList.get(coordList.size() - 1).x) {
                        log.trace("adding slice " + coordList.get(0).x);
                        coordinateLists.add(coordList);
                    } else {
                        removes.add(coordList);
                    }
                }
            }
            // Removes is a list of all the coordinate lists in the given 'side' where the first and last coordinate
            // don't have matching longitude values.
            if (removes.size() > 0) {
                log.trace("Sides1, size >1, total removes:" + removes.size());
                List<List<Coordinate>> removeResult = attach(removes, tops);
                if (removeResult.isEmpty()) {
                    //see if top/bottom intersect
                    if (tops.size() == 1 && bottoms.size() == 1) {
                        Coordinate inter = intersection(bottoms.get(0), tops.get(0));
                        if (inter != null) {
                            log.trace("bottom and top coordinate lists intersect");
                            List<Coordinate> merge = mergeLines(bottoms.get(0), tops.get(0), inter);
                            List<List<Coordinate>> llc = new ArrayList<>();
                            llc.add(merge);
                            removeResult = attach(removes, llc);
                            coordinateLists.addAll(removeResult);
                        }
                    }
                } else {
                    coordinateLists.addAll(removeResult);
                }
            }
        }
    }
    
    /**
     * Call processRemoves on the provided coordinate lists and log a warning if the result is empty.
     */
    private List<List<Coordinate>> attach(List<List<Coordinate>> removes,
                                          List<List<Coordinate>> connector) {
        List<List<Coordinate>> newList = processRemoves(removes, connector);
        //probably an issue here...
        if (newList.isEmpty())
            log.warn("processRemoves returned an empty list");
        return newList;
    }
    
    /**
     * Using the merged list of coordinates representing each side of the data (sides, top, bottom), generate a
     * Polygon or MultiPolygon.
     *
     * @param coords    The merged list of coords
     * @param tolerance Tolerance to use in the simplifier algorithm.
     * @return Footprint geometry
     */
    @Override
    public Geometry mergeGeoms(List<List<Coordinate>> coords, double tolerance) {
        log.trace("coords: " + coords.size());
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(1000d));
        
        List<List<Coordinate>> slices = new ArrayList<>();
        
        for (List<Coordinate> coordList : coords) {
            if (!coordList.get(0).equals(coordList.get(coordList.size() - 1))) {
                // same longitude at beginning/end
                if (coordList.get(0).x == coordList.get(coordList.size() - 1).x) {
                    log.debug("adding slice " + coordList.get(0).x);
                    slices.add(coordList);
                }
            }
            // Otherwise, first intersects last, so this represents a hole
        }
        coords.removeAll(slices);
        List<Coordinate> globalBox = constructGlobalBox(slices);
        
        LinearRing linearRing = geometryFactory.createLinearRing(globalBox.toArray(new Coordinate[0]));
        LinearRing[] holes = new LinearRing[coords.size()];
        
        for (int i = 0; i < coords.size(); i++) {
            log.trace("Creating hole.");
            List<Coordinate> coordinateList = coords.get(i);
            
            LinearRing holeLinearRing = geometryFactory.createLinearRing(coordinateList.toArray(new Coordinate[0]));
            holeLinearRing = (LinearRing) DouglasPeuckerSimplifier.simplify(holeLinearRing, tolerance);
            holes[i] = holeLinearRing;
        }
        log.trace("holes: " + holes.length);
        Polygon polygon = geometryFactory.createPolygon(linearRing, holes);
        Geometry geometry = DouglasPeuckerSimplifier.simplify(polygon, tolerance);
        
        log.trace("geometry: " + geometry.toText());
        
        if (geometry instanceof Polygon) {
            polygon = (Polygon) geometry;
            polygon = (Polygon) injectPoints(polygon);
            return polygon;
        } else {
            MultiPolygon multiPolygon = (MultiPolygon) geometry;
            
            Polygon[] polygons = new Polygon[multiPolygon.getNumGeometries()];
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                Polygon polygonGeometryN = (Polygon) multiPolygon.getGeometryN(i);
                polygonGeometryN = (Polygon) injectPoints(polygonGeometryN);
                polygons[i] = polygonGeometryN;
            }
            multiPolygon = geometryFactory.createMultiPolygon(polygons);
            return multiPolygon;
        }
    }
    
    /**
     * Find the intersection between the lists in 'removes' and the lists in 'connector', and merge those lists
     * together at the computed intersection point
     */
    public List<List<Coordinate>> processRemoves(List<List<Coordinate>> removes, List<List<Coordinate>> connector) {
        List<List<Coordinate>> rets = new ArrayList<>();
        log.trace("Remove size: " + removes.size());
        if (removes.size() == 1) {
            List<Coordinate> side = removes.get(0);
            for (List<Coordinate> coordList : connector) {
                Coordinate coordinate = intersection(side, coordList);
                if (coordinate != null) {
                    Collections.reverse(coordList);
                    List<Coordinate> merged = mergeLines(side, coordList, coordinate);
                    rets.add(merged);
                }
            }
        }
        if (removes.size() == 2) {
            List<Coordinate> coordinateList1 = removes.get(0);
            List<Coordinate> coordinateList2 = removes.get(1);
            
            List<Coordinate> start = null;
            List<Coordinate> end = null;
            
            Coordinate lc1First = coordinateList1.get(0);
            Coordinate lc2First = coordinateList2.get(0);
            
            log.trace("coordinateList1 " + lc1First.x);
            if (lc1First.x == 180d || lc1First.x == -180d) {
                start = coordinateList1;
            } else {
                end = coordinateList1;
            }
            
            log.trace("coordinateList2 " + lc2First.x);
            if (lc2First.x == 180d || lc2First.x == -180d) {
                start = coordinateList2;
            } else {
                end = coordinateList2;
            }
            
            if (start != null && end != null) {
                if (start.get(0).x == end.get(end.size() - 1).x) {
                    List<Coordinate> mergedList = mergeLists(start, end);
                    if (mergedList == null) {
                        log.trace("Look for connector intersections");
                        Coordinate coordinate = null;
                        for (List<Coordinate> coordList : connector) {
                            coordinate = intersection(start, coordList);
                            if (coordinate != null) {
                                log.trace("NotNull!: " + coordinate);
                                int index = findClosest(start, coordinate);
                                start = start.subList(0, index);
                                index = findClosest(coordList, coordinate);
                                log.trace(String.valueOf(index));
                                coordList = coordList.subList(0, index);
                                Collections.reverse(coordList);
                                start.addAll(coordList);
                                start.addAll(end);
                                rets.add(start);
                                break;
                            }
                        }
                        if (coordinate == null) {
                            log.error("Unable to find a point that intersects between the two lines.");
                        }
                    } else {
                        rets.add(start);
                    }
                } else {
                    log.trace("The first and last points in the start and end coordinate lists do not match.");
                    start.add(new Coordinate(start.get(0).x, start.get(start.size() - 1).y));
                    Coordinate add = new Coordinate(end.get(end.size() - 1).x, end.get(0).y);
                    end.add(0, add);
                    rets.add(start);
                    rets.add(end);
                }
            }
        }
        return rets;
    }
    
    /**
     * Find coordinate at intersection of line1 and line2, or return null if they don't intersect.
     */
    private Coordinate intersection(List<Coordinate> line1,
                                    List<Coordinate> line2) {
        
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(1000d));
        
        LineString lineString1 = geometryFactory.createLineString(line1.toArray(new Coordinate[0]));
        LineString lineString2 = geometryFactory.createLineString(line2.toArray(new Coordinate[0]));
        return lineString1.intersection(lineString2).getCoordinate();
    }
    
    
    /**
     * Merge the provided coordinate lists together, but finding the intersection point of the two lists and merging.
     */
    public List<Coordinate> mergeLists(List<Coordinate> start, List<Coordinate> end) {
        
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(1000d));
        
        LineString lsStart = geometryFactory.createLineString(start.toArray(new Coordinate[0]));
        LineString lsEnd = geometryFactory.createLineString(end.toArray(new Coordinate[0]));
        
        if (lsStart.intersects(lsEnd)) {
            try {
                Point point = (Point) lsStart.intersection(lsEnd);
                log.trace("Intersection at " + point);
                return mergeLines(start, end, point.getCoordinate());
            }
            catch(ClassCastException exc){
                // There is a multipoint intersection so return null to add all the points together
                log.debug("Exception during mergeLists Multipoint intersection FootprintStrategyPolar " + exc);
                return null;
            }
        } else {
            log.trace("NO Intersection!");
            return null;
        }
    }
    
    /**
     * Take 2 intersecting lines and make them one.
     */
    public List<Coordinate> mergeLines(List<Coordinate> start,
                                       List<Coordinate> end, Coordinate coordinate) {
        int index = findClosest(start, coordinate);
        List<Coordinate> mergedLines = new ArrayList<>(start.subList(0, index));
        index = findClosest(end, coordinate);
        mergedLines.addAll(end.subList(index, end.size()));
        return mergedLines;
    }
    
    public List<Coordinate> constructGlobalBox(List<List<Coordinate>> slices) {
        List<Coordinate> globalBox = new ArrayList<>();
        
        globalBox.add(new Coordinate(180d, 90d));
        // Check for slices
        for (List<Coordinate> slice : slices) {
            if (slice.get(0).x == 180d) {
                // Check for descending lat
                if (!(slice.get(0).y > slice.get(slice.size() - 1).y)) {
                    Collections.reverse(slice);
                }
                globalBox.addAll(slice);
            }
        }
        
        globalBox.add(new Coordinate(180d, -90d));
        globalBox.add(new Coordinate(-180d, -90d));
        // Check for slices
        for (List<Coordinate> slice : slices) {
            if (slice.get(0).x == -180d) {
                // Check for ascending lat
                if (!(slice.get(0).y < slice.get(slice.size() - 1).y)) {
                    Collections.reverse(slice);
                }
                globalBox.addAll(slice);
            }
        }
        
        globalBox.add(new Coordinate(-180d, 90d));
        globalBox.add(new Coordinate(180d, 90d));
        return globalBox;
    }
}
