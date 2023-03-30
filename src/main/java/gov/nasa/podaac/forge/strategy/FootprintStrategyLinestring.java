package gov.nasa.podaac.forge.strategy;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Footprint strategy to be used by the {@link gov.nasa.podaac.forge.Footprinter} which specifies how the footprint
 * is generated.
 * <p>
 * In this case, split on the -180:180 transition (with a bit of a fudge factor of .2)
 */
public class FootprintStrategyLinestring extends FootprintStrategy {
    private static final Logger log = LoggerFactory.getLogger(FootprintStrategyLinestring.class);
    private List<List<Coordinate>> coordinateList = new ArrayList<>();
    
    
    /**
     * In this case, just split side1 and return. Other params are ignored.
     */
    @Override
    public List<List<Coordinate>> merge(List<Coordinate> side1,
                                        List<Coordinate> bottom, List<Coordinate> side2,
                                        List<Coordinate> top) {
        List<List<Coordinate>> sides1 = split(side1);
        coordinateList = new ArrayList<>(sides1);
        return coordinateList;
    }
    
    /**
     * Merge list of coordinates into a LineString geometry
     *
     * @param coords    List of coordinates used to generate geometry
     * @param tolerance Tolerance used in simplification algorithm
     * @return Computed Geometry. IN this case, will be a LineString.
     */
    public Geometry mergeGeoms(List<List<Coordinate>> coords, double tolerance) {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(1000d));
        List<Geometry> geometries = new ArrayList<>();
        for (List<Coordinate> lc : coords) {
            LineString lineString = gf.createLineString(lc.toArray(new Coordinate[0]));
            Geometry geometry = DouglasPeuckerSimplifier.simplify(lineString, tolerance);
            if (!geometry.toText().contains("EMPTY")) {
                log.debug(geometry.toText());
                geometries.add(geometry);
            }
        }
        if (geometries.size() > 1) {
            return gf.createMultiLineString(geometries.toArray(new LineString[0]));
        }
        return geometries.get(0);
    }
}
