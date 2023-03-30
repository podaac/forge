package gov.nasa.podaac.forge.strategy;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import gov.nasa.podaac.forge.FootprintException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class FootprintStrategyPolarSidesOnlyTest {

    private final FootprintStrategyPolarSidesOnly footprintStrategyPolarSidesOnly = new FootprintStrategyPolarSidesOnly();
    
    /**
     * Ensure a valid geometry is created from a set of manually calculated points, where side1 is split but side2
     * is not.
     */
    @Test
    public void testPolarSide1Split() throws FootprintException {
        List<Coordinate> s1 = new ArrayList<>();
        List<Coordinate> s2 = new ArrayList<>();
        List<Coordinate> t = new ArrayList<>();
        List<Coordinate> b = new ArrayList<>();
        
        s1.add(new Coordinate(-175, 75));
        s1.add(new Coordinate(-150, 75));
        s1.add(new Coordinate(-120, 60));
        s1.add(new Coordinate(-90, 30));
        s1.add(new Coordinate(-90, 0));
        s1.add(new Coordinate(-90, -30));
        s1.add(new Coordinate(-120, -60));
        s1.add(new Coordinate(-150, -75));
        s1.add(new Coordinate(-175, -75));
        s1.add(new Coordinate(175, -75));
        s1.add(new Coordinate(150, -75));
        s1.add(new Coordinate(120, -60));
        s1.add(new Coordinate(90, -30));
        s1.add(new Coordinate(90, 0));
        s1.add(new Coordinate(90, 30));
        s1.add(new Coordinate(120, 60));
        s1.add(new Coordinate(150, 75));
        s1.add(new Coordinate(175, 75));
        
        s2.add(new Coordinate(0, 60));
        s2.add(new Coordinate(-45, 45));
        s2.add(new Coordinate(-60, 0));
        s2.add(new Coordinate(-45, -45));
        s2.add(new Coordinate(0, -60));
        s2.add(new Coordinate(45, -45));
        s2.add(new Coordinate(60, 0));
        s2.add(new Coordinate(45, 45));
        s2.add(new Coordinate(0, 60));
        
        t.add(new Coordinate(0, 90));
        t.add(new Coordinate(0, 75));
        t.add(new Coordinate(0, 60));
        
        b.add(new Coordinate(0, 90));
        b.add(new Coordinate(0, 75));
        b.add(new Coordinate(0, 60));
        
        List<List<Coordinate>> coordsList = footprintStrategyPolarSidesOnly.merge(s1, b, s2, t);
        Geometry geometry = footprintStrategyPolarSidesOnly.mergeGeoms(coordsList, 0.5);
        assertTrue(geometry.isValid());
    }
    
    /**
     * Ensure a valid geometry is created from a set of manually calculated points, where side1 is not split and side2
     * is.
     */
    @Test
    public void testPolarSide2Split() throws FootprintException {
        List<Coordinate> s1 = new ArrayList<>();
        List<Coordinate> s2 = new ArrayList<>();
        List<Coordinate> t = new ArrayList<>();
        List<Coordinate> b = new ArrayList<>();
        
        s2.add(new Coordinate(-175, 75));
        s2.add(new Coordinate(-150, 75));
        s2.add(new Coordinate(-120, 60));
        s2.add(new Coordinate(-90, 30));
        s2.add(new Coordinate(-90, 0));
        s2.add(new Coordinate(-90, -30));
        s2.add(new Coordinate(-120, -60));
        s2.add(new Coordinate(-150, -75));
        s2.add(new Coordinate(-175, -75));
        s2.add(new Coordinate(175, -75));
        s2.add(new Coordinate(150, -75));
        s2.add(new Coordinate(120, -60));
        s2.add(new Coordinate(90, -30));
        s2.add(new Coordinate(90, 0));
        s2.add(new Coordinate(90, 30));
        s2.add(new Coordinate(120, 60));
        s2.add(new Coordinate(150, 75));
        s2.add(new Coordinate(175, 75));
        
        s1.add(new Coordinate(0, 60));
        s1.add(new Coordinate(-45, 45));
        s1.add(new Coordinate(-60, 0));
        s1.add(new Coordinate(-45, -45));
        s1.add(new Coordinate(0, -60));
        s1.add(new Coordinate(45, -45));
        s1.add(new Coordinate(60, 0));
        s1.add(new Coordinate(45, 45));
        s1.add(new Coordinate(0, 60));
        
        t.add(new Coordinate(0, 90));
        t.add(new Coordinate(0, 75));
        t.add(new Coordinate(0, 60));
        
        b.add(new Coordinate(0, 90));
        b.add(new Coordinate(0, 75));
        b.add(new Coordinate(0, 60));
        
        List<List<Coordinate>> coordsList = footprintStrategyPolarSidesOnly.merge(s1, b, s2, t);
        Geometry geometry = footprintStrategyPolarSidesOnly.mergeGeoms(coordsList, 0.5);
        assertTrue(geometry.isValid());
    }
    
    /**
     * Ensure a valid geometry is created from a set of manually calculated points, where side2 does not
     * intersect itself, but intersects tops
     */
    @Test
    public void testPolarNoIntersect() throws FootprintException {
        List<Coordinate> s1 = new ArrayList<>();
        List<Coordinate> s2 = new ArrayList<>();
        List<Coordinate> t = new ArrayList<>();
        List<Coordinate> b = new ArrayList<>();
        
        s1.add(new Coordinate(-175, 75));
        s1.add(new Coordinate(-150, 75));
        s1.add(new Coordinate(-120, 60));
        s1.add(new Coordinate(-90, 30));
        s1.add(new Coordinate(-90, 0));
        s1.add(new Coordinate(-90, -30));
        s1.add(new Coordinate(-120, -60));
        s1.add(new Coordinate(-150, -75));
        s1.add(new Coordinate(-175, -75));
        s1.add(new Coordinate(175, -75));
        s1.add(new Coordinate(150, -75));
        s1.add(new Coordinate(120, -60));
        s1.add(new Coordinate(90, -30));
        s1.add(new Coordinate(90, 0));
        s1.add(new Coordinate(90, 30));
        s1.add(new Coordinate(120, 60));
        s1.add(new Coordinate(150, 75));
        s1.add(new Coordinate(175, 75));
        
        s2.add(new Coordinate(0, 60));
        s2.add(new Coordinate(-45, 45));
        s2.add(new Coordinate(-60, 0));
        s2.add(new Coordinate(-45, -45));
        s2.add(new Coordinate(0, -60));
        s2.add(new Coordinate(45, -45));
        s2.add(new Coordinate(60, 0));
        s2.add(new Coordinate(45, 45));
        
        t.add(new Coordinate(0, 90));
        t.add(new Coordinate(0, 75));
        t.add(new Coordinate(0, 60));
        
        // Add intersecting point
        s2.add(new Coordinate(0, 90));
        
        b.add(new Coordinate(0, 90));
        b.add(new Coordinate(0, 75));
        b.add(new Coordinate(0, 60));
        
        List<List<Coordinate>> coordsList = footprintStrategyPolarSidesOnly.merge(s1, b, s2, t);
        Geometry geometry = footprintStrategyPolarSidesOnly.mergeGeoms(coordsList, 0.5);
        assertTrue(geometry.isValid());
    }
}
