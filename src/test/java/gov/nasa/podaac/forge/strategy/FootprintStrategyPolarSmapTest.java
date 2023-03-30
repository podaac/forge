package gov.nasa.podaac.forge.strategy;

import com.vividsolutions.jts.geom.Coordinate;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ucar.ma2.Array;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class FootprintStrategyPolarSmapTest extends TestCase {
    
    private final FootprintStrategyPolarSmap footprintStrategyPolarSmap = new FootprintStrategyPolarSmap();
    
    /**
     * Setup mocks for testing and run calculateFootprint with the given params
     */
    private List<List<Coordinate>> runCalculateFootprint(float[][][] lonData, float[][][] latData, boolean is360, boolean removeOrigin, boolean findValid, double scale, double offset) throws IOException {
        Variable lonVariable = Mockito.mock(Variable.class);
        Variable latVariable = Mockito.mock(Variable.class);
        Map<String, Double> latAttMap = new HashMap<>();
        Map<String, Double> lonAttMap = new HashMap<>();
        List<Coordinate> side1 = new ArrayList<>();
        List<Coordinate> side2 = new ArrayList<>();
        List<Coordinate> top = new ArrayList<>();
        List<Coordinate> bottom = new ArrayList<>();
    
        lonAttMap.put("fill", Double.MAX_VALUE);
        latAttMap.put("fill", Double.MAX_VALUE);
        lonAttMap.put("scale", scale);
        latAttMap.put("scale", scale);
        lonAttMap.put("offset", offset);
        latAttMap.put("offset", offset);
    
        Array mockLonArray = Mockito.mock(Array.class);
        Array mockLatArray = Mockito.mock(Array.class);
    
        Mockito.doReturn(lonData)
                .when(mockLonArray)
                .copyToNDJavaArray();
    
        Mockito.doReturn(latData)
                .when(mockLatArray)
                .copyToNDJavaArray();
    
        Mockito.doReturn(mockLonArray)
                .when(lonVariable)
                .read();
    
        Mockito.doReturn(mockLatArray)
                .when(latVariable)
                .read();
    
        footprintStrategyPolarSmap.calculateFootprint(lonVariable, latVariable, latAttMap, lonAttMap, side1, side2,
                top, bottom, is360, findValid, removeOrigin);
        return Arrays.asList(side1, side2, top, bottom);
    }
    
    /**
     * Test that the FootprintStrategyPolarSmap calculateFootprint function behaves as expected, and that the side1,
     * top, and bottom Coordinate lists contain the expected values.
     */
    @Test
    public void testCalculateFootprint() throws IOException {
        float[][][] lonData = new float[][][]{
            {{10}, {20}, {30}, {40}, {50}, {60}, {70}, {80}, {90}, {100}},
            {{110}, {120}, {130}, {140}, {150}, {160}, {170}, {180}, {190}, {200}}
        };
        float[][][] latData = new float[][][]{
                {{5}, {10}, {15}, {20}, {25}, {30}, {35}, {40}, {45}, {50}},
                {{55}, {60}, {65}, {70}, {75}, {80}, {85}, {90}, {95}, {100}}
        };
    
        List<List<Coordinate>> result = runCalculateFootprint(lonData, latData, false, false, false, 1.0, 0.0);
    
        List<Coordinate> side1 = result.get(0);
        List<Coordinate> side2 = result.get(1);
        List<Coordinate> top = result.get(2);
        List<Coordinate> bottom = result.get(3);
                                                      
        assertEquals(side1.size(), 2);
        assertEquals(side1.get(0).x, (double) lonData[0][0][0]);
        assertEquals(side1.get(0).y, (double) latData[0][0][0]);
        
        assertEquals(side2.size(), 0);
        
        // Would be 10, but two points are out of range (-180/180) so they should be removed.
        assertEquals(top.size(), 8);
        for (int i = 0; i < 8; i++) {
            Coordinate coord = top.get(i);
            assertEquals(coord.x, (double) lonData[1][i][0]);
            assertEquals(coord.y, (double) latData[1][i][0]);
        }
    
        assertEquals(bottom.size(), 10);
        for (int i = 0; i < 10; i++) {
            Coordinate coord = bottom.get(i);
            assertEquals(coord.x, (double) lonData[0][i][0]);
            assertEquals(coord.y, (double) latData[0][i][0]);
        }
    }
    
    /**
     * Test that the FootprintStrategyPolarSmap calculateFootprint function behaves as expected, and that the side1,
     * top, and bottom Coordinate lists contain the expected values.
     *
     * This case will test an example where is360 is true, so lon values above 180 should be transformed to -180/180 form
     */
    @Test
    public void testCalculateFootprintIs360() throws IOException {
        float[][][] lonData = new float[][][]{
                {{10}, {20}, {30}, {40}, {50}, {60}, {70}, {80}, {90}, {100}},
                {{110}, {120}, {130}, {140}, {150}, {160}, {170}, {180}, {190}, {200}}
        };
        float[][][] latData = new float[][][]{
                {{5}, {10}, {15}, {20}, {25}, {30}, {35}, {40}, {45}, {50}},
                {{55}, {60}, {65}, {70}, {75}, {80}, {85}, {90}, {90}, {90}}
        };
    
        List<List<Coordinate>> result = runCalculateFootprint(lonData, latData, true, false, false, 1.0, 0.0);
    
        List<Coordinate> side1 = result.get(0);
        List<Coordinate> side2 = result.get(1);
        List<Coordinate> top = result.get(2);
        List<Coordinate> bottom = result.get(3);
        
        assertEquals(side1.size(), 2);
        assertEquals(side2.size(), 0);
        assertEquals(top.size(), 10);
        assertEquals(bottom.size(), 10);
        
        assertEquals(top.get(8).x , (double) -170);
        assertEquals(top.get(9).x , (double) -160);
    }
    
    /**
     * Test that the FootprintStrategyPolarSmap calculateFootprint function behaves as expected, and that the side1,
     * top, and bottom Coordinate lists contain the expected values.
     *
     * This case will test an example where removeOrigin is true, so points that are (0, 0) should not be present in
     * the result.
     */
    @Test
    public void testCalculateFootprintRemoveOrigin() throws IOException {
        float[][][] lonData = new float[][][]{
                {{10}, {20}, {30}, {40}, {50}, {60}, {70}, {80}, {90}, {0}},
                {{110}, {120}, {130}, {140}, {150}, {160}, {170}, {180}, {190}, {200}}
        };
        float[][][] latData = new float[][][]{
                {{5}, {10}, {15}, {20}, {25}, {30}, {35}, {40}, {45}, {0}},
                {{55}, {60}, {65}, {70}, {75}, {80}, {85}, {90}, {90}, {90}}
        };
    
        List<List<Coordinate>> result = runCalculateFootprint(lonData, latData, false, true, false, 1.0, 0.0);
    
        List<Coordinate> side1 = result.get(0);
        List<Coordinate> side2 = result.get(1);
        List<Coordinate> top = result.get(2);
        List<Coordinate> bottom = result.get(3);
    
        assertEquals(side1.size(), 2);
        assertEquals(side2.size(), 0);
        assertEquals(top.size(), 8);
        assertEquals(bottom.size(), 9);
        
        assertEquals(bottom.get(8).x, (double) 90);
        assertEquals(bottom.get(8).y, (double) 45);
    }
    
    @Test
    public void testCalculateFootprintFindValid() throws IOException {
        float fill = (float) Double.MAX_VALUE;
        
        float[][][] lonData = new float[][][]{
                {{10}, {20}, {30}, {40}, {50}, {60}, {70}, {80}, {90}, {fill}},
                {{110}, {120}, {130}, {140}, {150}, {160}, {170}, {180}, {190}, {200}}
        };
        float[][][] latData = new float[][][]{
                {{5}, {10}, {15}, {20}, {25}, {30}, {35}, {40}, {45}, {fill}},
                {{55}, {60}, {65}, {70}, {75}, {80}, {85}, {90}, {90}, {90}}
        };
    
        List<List<Coordinate>> result = runCalculateFootprint(lonData, latData, false, true, false, 1.0, 0.0);
    
        List<Coordinate> side1 = result.get(0);
        List<Coordinate> side2 = result.get(1);
        List<Coordinate> top = result.get(2);
        List<Coordinate> bottom = result.get(3);
    
        assertEquals(side1.size(), 2);
        assertEquals(side2.size(), 0);
        assertEquals(top.size(), 8);
        assertEquals(bottom.size(), 9);
    }
    
    @Test
    public void testCalculateFootprintScaleOffset() throws IOException {
        float[][][] lonData = new float[][][]{
                {{10}, {20}, {30}, {40}, {50}, {60}, {70}, {80}, {90}, {100}},
                {{110}, {120}, {130}, {140}, {150}, {160}, {170}, {180}, {190}, {200}}
        };
        float[][][] latData = new float[][][]{
                {{5}, {10}, {15}, {20}, {25}, {30}, {35}, {40}, {45}, {50}},
                {{55}, {60}, {65}, {70}, {75}, {80}, {85}, {90}, {90}, {90}}
        };
        
        List<List<Coordinate>> result = runCalculateFootprint(lonData, latData, false, false, false, 2.0, 1.0);
        
        List<Coordinate> side1 = result.get(0);
        List<Coordinate> side2 = result.get(1);
        List<Coordinate> top = result.get(2);
        List<Coordinate> bottom = result.get(3);
        
        assertEquals(side1.size(), 1);
        assertEquals(side2.size(), 0);
        // Tops should be empty because all the values are outside the valid range now (after applying scale and offset)
        assertEquals(top.size(), 0);
        // Bottom is only 8 because two of the values are outside the valid range when offset and scale are applied
        assertEquals(bottom.size(), 8);
    
        assertEquals(side1.get(0).x, (double) (lonData[0][0][0] * 2 + 1));
        assertEquals(side1.get(0).y, (double) (latData[0][0][0] * 2 + 1));
    
        for (int i = 0; i < 8; i++) {
            Coordinate coord = bottom.get(i);
            assertEquals(coord.x, (double) (lonData[0][i][0] * 2 + 1));
            assertEquals(coord.y, (double) (latData[0][i][0] * 2 + 1));
        }
    }
}
