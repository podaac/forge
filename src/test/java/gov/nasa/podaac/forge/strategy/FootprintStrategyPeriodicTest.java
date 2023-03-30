package gov.nasa.podaac.forge.strategy;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import gov.nasa.podaac.forge.FootprintException;
import gov.nasa.podaac.forge.FootprintTestUtil;
import junit.framework.TestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FootprintStrategyPeriodicTest extends TestCase {
    
    private final FootprintStrategyPeriodic footprintStrategyPeriodic = new FootprintStrategyPeriodic();
    
    /**
     * standard, non split polygon.
     */
    @Test
    public void testNormal() throws FootprintException {
        List<Coordinate> s1 = new ArrayList<>();
        List<Coordinate> s2 = new ArrayList<>();
        List<Coordinate> t = new ArrayList<>();
        List<Coordinate> b = new ArrayList<>();
        
        s1.add(new Coordinate(60d, 10d));
        s1.add(new Coordinate(50d, 5d));
        s1.add(new Coordinate(40d, 0d));
        
        s2.add(new Coordinate(50d, 10d));
        s2.add(new Coordinate(40d, 5d));
        s2.add(new Coordinate(30d, 0d));
        
        t.add(new Coordinate(60d, 10d));
        t.add(new Coordinate(55d, 10d));
        t.add(new Coordinate(50d, 10d));
        
        b.add(new Coordinate(40d, 0d));
        b.add(new Coordinate(45d, 0d));
        b.add(new Coordinate(30d, 0d));
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        assertEquals(1, lc.size());
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertTrue(g.isValid());
    }
    
    /**
     * Standard split from 180 side to -180 side.
     */
    @Test
    public void testSplit1() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(170d, 10d, 190d, -10d, 1000);
        List<Coordinate> s2 = FootprintTestUtil.genList(175d, 10d, 195d, -10d, 1000);
        List<Coordinate> t = new ArrayList<>();
        List<Coordinate> b = new ArrayList<>();
        
        t.add(new Coordinate(170d, 10d));
        t.add(new Coordinate(175d, 10d));
        
        b.add(new Coordinate(-170d, -10d));
        b.add(new Coordinate(-165d, -10d));
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        assertEquals(2, lc.size());
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertTrue(g.isValid());
    }
    
    /**
     * Standard split from -180 side to 180 side.
     */
    @Test
    public void testSplit2() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(195d, 10d, 175d, -10d, 1000);
        List<Coordinate> s2 = FootprintTestUtil.genList(190d, 10d, 170d, -10d, 1000);
        List<Coordinate> t = new ArrayList<>();
        List<Coordinate> b = new ArrayList<>();
        
        t.add(new Coordinate(-165d, 10d));
        t.add(new Coordinate(-170d, 10d));
        
        b.add(new Coordinate(175d, -10d));
        b.add(new Coordinate(170d, -10d));
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        assertEquals(2, lc.size());
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertTrue(g.isValid());
    }
    
    /**
     * Complicated split with top and bottom being split, not the sides
     */
    @Test
    public void testSplit3() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(165d, 10d, 175d, -10d, 100);
        List<Coordinate> s2 = FootprintTestUtil.genList(185d, 10d, 195d, -10d, 100);
        List<Coordinate> t = FootprintTestUtil.genList(165d, 10d, 185d, 10d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(175d, -10d, 195d, -10d, 30);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5);
        assertEquals(2, lc.size());
        assertTrue(g.isValid());
    }
    
    /**
     * Complicated split with top and bottom being split, not the sides
     */
    @Test
    public void testSplit4() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(195d, 10d, 185d, -10d, 100);
        List<Coordinate> s2 = FootprintTestUtil.genList(175d, 10d, 165d, -10d, 100);
        List<Coordinate> t = FootprintTestUtil.genList(195d, 10d, 175d, 10d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(185d, -10d, 165d, -10d, 30);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(2, lc.size());
        assertTrue(g.isValid());
    }
    
    /**
     * Complicated split with top and bottom being split, and each side being split once
     */
    @Test
    public void testSplit5() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(170, 40d, 520d, -40d, 1000);
        List<Coordinate> s2 = FootprintTestUtil.genList(-160d, 40d, 190d, -40d, 1000);
        List<Coordinate> t = FootprintTestUtil.genList(170d, 40d, 200d, 40d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(160d, -40d, 200d, -40d, 30);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(3, lc.size());
        assertTrue(g.isValid());
    }
    
    /**
     * Complicated split with top and bottom being split, and each side being split once
     */
    @Test
    public void testSplit6() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(170, -40d, 530d, 40d, 1000);
        List<Coordinate> s2 = FootprintTestUtil.genList(-160d, -40d, 200d, 40d, 1000);
        List<Coordinate> t = FootprintTestUtil.genList(170d, 40d, 200d, 40d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(170d, -40d, 200d, -40d, 30);
        
        Collections.reverse(s1);
        Collections.reverse(s2);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(3, lc.size());
        assertTrue(g.isValid());
        
    }
    
    /**
     * Complicated split with top and bottom being split, and each side being split once
     */
    @Test
    public void testSplit7() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(170, -40d, 520d, 40d, 1000);
        List<Coordinate> s2 = FootprintTestUtil.genList(-160d, -40d, 190d, 40d, 1000);
        List<Coordinate> t = FootprintTestUtil.genList(170d, -40d, 200d, -40d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(160d, 40d, 200d, 40d, 30);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(3, lc.size());
        assertTrue(g.isValid());
    }
    
    /**
     * Complicated split with top and bottom being split, and each side being split once
     */
    @Test
    public void testSplit8() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(170, 40d, 530d, -40d, 1000);
        List<Coordinate> s2 = FootprintTestUtil.genList(-160d, 40d, 200d, -40d, 1000);
        List<Coordinate> t = FootprintTestUtil.genList(170d, -40d, 200d, -40d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(170d, 40d, 200d, 40d, 30);
        
        Collections.reverse(s1);
        Collections.reverse(s2);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(3, lc.size());
        assertTrue(g.isValid());
    }
    
    
    /**
     * Complicated split with top being split, but not bottom, and an odd number of sides
     */
    @Test
    public void testSplit9() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(170, -40d, 530d, 40d, 100);
        List<Coordinate> s2 = FootprintTestUtil.genList(175d, -40d, 545d, 40d, 100);
        List<Coordinate> t = FootprintTestUtil.genList(170d, 40d, 185d, 40d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(170d, -40d, 175d, -40d, 30);
        
        Collections.reverse(s1);
        Collections.reverse(s2);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(3, lc.size());
        assertTrue(g.isValid());
    }
    
    /**
     * Complicated split with top being split, but not bottom, and an odd number of sides
     */
    @Test
    public void testSplit10() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(170, -40d, 530d, 40d, 100);
        List<Coordinate> s2 = FootprintTestUtil.genList(175d, -40d, 545d, 40d, 100);
        List<Coordinate> t = FootprintTestUtil.genList(175d, -40d, 175d, -40d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(170d, 40d, 185d, 40d, 30);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(3, lc.size());
        assertTrue(g.isValid());
        
    }
    
    @Test
    public void testSplit11() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(175d, -40d, 545d, 40d, 100);
        List<Coordinate> s2 = FootprintTestUtil.genList(170, -40d, 530d, 40d, 100);
        List<Coordinate> t = FootprintTestUtil.genList(175d, -40d, 175d, -40d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(170d, 40d, 185d, 40d, 30);
        
        Collections.reverse(b);
        Collections.reverse(t);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(3, lc.size());
        assertTrue(g.isValid());
    }
    
    
    /**
     * Uneven sides, split top first is the small polygon
     */
    @Test
    public void testSplit12() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(170d, 40d, 550d, -40d, 100);
        List<Coordinate> s2 = FootprintTestUtil.genList(-170, 40d, 200d, -40d, 100);
        List<Coordinate> t = FootprintTestUtil.genList(170d, 40d, 190d, 40d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(-170d, -40d, -160d, -40d, 30);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(3, lc.size());
        assertTrue(g.isValid());
    }
    
    /**
     * Uneven sides, split top first is the small polygon
     */
    @Test
    public void testSplit13() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(160d, 40d, 155d, -40d, 100);
        List<Coordinate> s2 = FootprintTestUtil.genList(-177, 40d, -180, -40d, 100);
        List<Coordinate> t = FootprintTestUtil.genList(160d, 40d, 183d, 40d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(155d, -40d, 180d, -40d, 30);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(2, lc.size());
        assertTrue(g.isValid());
    }
    
    @Test
    public void testSplit14() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(-177, 40d, -180, -40d, 100);
        List<Coordinate> s2 = FootprintTestUtil.genList(160d, 40d, 155d, -40d, 100);
        List<Coordinate> t = FootprintTestUtil.genList(160d, 40d, 183d, 40d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(155d, -40d, 180d, -40d, 30);
        
        
        Collections.reverse(t);
        Collections.reverse(b);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(2, lc.size());
        assertTrue(g.isValid());
    }
    
    @Test
    public void testOddSidesNoTopSplit() throws FootprintException {
        List<Coordinate> s1 = FootprintTestUtil.genList(190d, 0d, 140, 40d, 100);
        List<Coordinate> s2 = FootprintTestUtil.genList(180, 0d, 120d, 40d, 100);
        List<Coordinate> t = FootprintTestUtil.genList(190d, 0d, 180d, 0d, 30);
        List<Coordinate> b = FootprintTestUtil.genList(140d, 40d, 120d, 40d, 30);
        
        List<List<Coordinate>> lc = footprintStrategyPeriodic.merge(s1, b, s2, t);
        
        Geometry g = footprintStrategyPeriodic.mergeGeoms(lc, .5d);
        assertEquals(2, lc.size());
        assertTrue(g.isValid());
    }
    
    @Test
    public void testBottomRowInvalid() {
        List<Coordinate> s1 = FootprintTestUtil.genList(176d, 90d, 179.999d, 90d, 10);
        List<Coordinate> s2 = FootprintTestUtil.genList(176d, 90d, 179.999d, 90d, 10);
        List<Coordinate> t = FootprintTestUtil.genList(179.999d, 90d, 179.999d, 90d, 10);
        List<Coordinate> b = FootprintTestUtil.genList(179.999d, 90d, 179.999d, 90d, 10);
    
        Assertions.assertThrows(FootprintException.class, () -> footprintStrategyPeriodic.merge(s1, b, s2, t));
    }
}
