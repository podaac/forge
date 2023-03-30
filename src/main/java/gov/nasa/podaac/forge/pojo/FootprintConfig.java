package gov.nasa.podaac.forge.pojo;

import com.google.gson.annotations.SerializedName;
import gov.nasa.podaac.forge.strategy.FootprintStrategy;

/**
 * Java class that maps to the PO.DAAC JSON dataset config 'footprint' field.
 */
public class FootprintConfig {

    private FootprintStrategy.Strategy strategy;
    private boolean findValid;
    private boolean removeOrigin;
    @SerializedName(value="t")
    private String top;
    @SerializedName(value="s1")
    private String side1;
    @SerializedName(value="b")
    private String bottom;
    @SerializedName(value="s2")
    private String side2;
    @SerializedName(value="geospatial_lat_min")
    private long geospatialLatMin;
    @SerializedName(value="geospatial_lat_max")
    private long geospatialLatMax;
    @SerializedName(value="geospatial_lon_min")
    private long geospatialLonMin;
    @SerializedName(value="geospatial_lon_max")
    private long geospatialLonMax;
    
    public FootprintConfig(FootprintStrategy.Strategy strategy, boolean findValid, boolean removeOrigin, String top,
                           String side1, String bottom, String side2, int geospatialLatMin, int geospatialLatMax, int geospatialLonMin,
                           int geospatialLonMax) {
        this.strategy = strategy;
        this.findValid = findValid;
        this.removeOrigin = removeOrigin;
        this.top = top;
        this.side1 = side1;
        this.bottom = bottom;
        this.side2 = side2;
        this.geospatialLatMin = geospatialLatMin;
        this.geospatialLatMax = geospatialLatMax;
        this.geospatialLonMin = geospatialLonMin;
        this.geospatialLonMax = geospatialLonMax;
    }
    
    public FootprintConfig() {
        this.removeOrigin = false;
        this.findValid = false;
        this.strategy = FootprintStrategy.Strategy.PERIODIC;
    }
    
    public FootprintStrategy.Strategy getStrategy() {
        return strategy;
    }
    
    public void setStrategy(FootprintStrategy.Strategy strategy) {
        this.strategy = strategy;
    }
    
    public void setStrategy(String strategy) {
        this.strategy = FootprintStrategy.Strategy.fromString(strategy);
    }
    
    public String getTop() {
        return top;
    }
    
    public void setTop(String top) {
        this.top = top;
    }
    
    public String getSide1() {
        return side1;
    }
    
    public void setSide1(String side1) {
        this.side1 = side1;
    }
    
    public String getBottom() {
        return bottom;
    }
    
    public void setBottom(String bottom) {
        this.bottom = bottom;
    }
    
    public String getSide2() {
        return side2;
    }
    
    public void setSide2(String side2) {
        this.side2 = side2;
    }
    
    public boolean isFindValid() {
        return findValid;
    }
    
    public void setFindValid(boolean findValid) {
        this.findValid = findValid;
    }
    
    public boolean isRemoveOrigin() {
        return removeOrigin;
    }
    
    public void setRemoveOrigin(boolean removeOrigin) {
        this.removeOrigin = removeOrigin;
    }
    
    public long getGeospatialLatMin() {
        return geospatialLatMin;
    }
    
    public void setGeospatialLatMin(long geospatialLatMin) {
        this.geospatialLatMin = geospatialLatMin;
    }
    
    public long getGeospatialLatMax() {
        return geospatialLatMax;
    }
    
    public void setGeospatialLatMax(long geospatialLatMax) {
        this.geospatialLatMax = geospatialLatMax;
    }
    
    public long getGeospatialLonMin() {
        return geospatialLonMin;
    }
    
    public void setGeospatialLonMin(long geospatialLonMin) {
        this.geospatialLonMin = geospatialLonMin;
    }
    
    public long getGeospatialLonMax() {
        return geospatialLonMax;
    }
    
    public void setGeospatialLonMax(long geospatialLonMax) {
        this.geospatialLonMax = geospatialLonMax;
    }
}
