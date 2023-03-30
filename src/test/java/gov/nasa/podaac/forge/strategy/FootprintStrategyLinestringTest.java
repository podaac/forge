package gov.nasa.podaac.forge.strategy;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class FootprintStrategyLinestringTest extends TestCase {
    
    private final FootprintStrategyLinestring footprintStrategyLinestring = new FootprintStrategyLinestring();
    
    /**
     * Merge should return the s1 side, unchanged, wrapped in another list.
     */
    @Test
    public void testMerge() {
        List<Coordinate> s1 = new ArrayList<>();
        s1.add(new Coordinate(60d, 10d));
        s1.add(new Coordinate(50d, 5d));
        s1.add(new Coordinate(40d, 0d));
        
        List<List<Coordinate>> coordsList = footprintStrategyLinestring.merge(s1, null, null, null);
        
        assert coordsList.size() == 1;
        assert coordsList.get(0).equals(s1);
    }
    
    /**
     * mergeGeoms should generate a valid LINESTRING geometry
     */
    @Test
    public void testMergeGeoms() {
        List<Coordinate> s1 = new ArrayList<>();
        s1.add(new Coordinate(60d, 10d));
        s1.add(new Coordinate(55d, 5d));
        s1.add(new Coordinate(40d, 0d));
        s1.add(new Coordinate(55d, -5d));
        s1.add(new Coordinate(60d, -10d));
        List<List<Coordinate>> coordsList = new ArrayList<>();
        coordsList.add(s1);
        
        Geometry geometry = footprintStrategyLinestring.mergeGeoms(coordsList, 0.5d);
        assertTrue(geometry.isValid());
        assertTrue(geometry.toText().contains("LINESTRING"));
    }
    
    /**
     * mergeGeoms should generate a valid MULTILINESTRING when more than one line is given in the input coordinates.
     */
    @Test
    public void testMergeGeomsMultiLine() {
        List<Coordinate> s1 = new ArrayList<>();
        s1.add(new Coordinate(60d, 10d));
        s1.add(new Coordinate(55d, 5d));
        s1.add(new Coordinate(40d, 0d));
        s1.add(new Coordinate(55d, -5d));
        s1.add(new Coordinate(60d, -10d));
        List<Coordinate> s2 = new ArrayList<>();
        s2.add(new Coordinate(-60d, 10d));
        s2.add(new Coordinate(-55d, 5d));
        s2.add(new Coordinate(-40d, 0d));
        s2.add(new Coordinate(-55d, -5d));
        s2.add(new Coordinate(-60d, -10d));
        List<List<Coordinate>> coordsList = new ArrayList<>();
        coordsList.add(s1);
        coordsList.add(s2);
        
        Geometry geometry = footprintStrategyLinestring.mergeGeoms(coordsList, 0.5d);
        assertTrue(geometry.isValid());
        assertTrue(geometry.toText().contains("MULTILINESTRING"));
    }
}
