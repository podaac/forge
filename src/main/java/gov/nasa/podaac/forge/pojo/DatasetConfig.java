package gov.nasa.podaac.forge.pojo;

/**
 * Java class that maps to the PO.DAAC JSON dataset config.
 */
public class DatasetConfig {
    private String latVar;
    private String lonVar;
    private boolean is360;
    private double tolerance;
    private FootprintConfig footprint;
    
    public DatasetConfig(String latVar, String lonVar, boolean is360, int tolerance, FootprintConfig footprint) {
        this.latVar = latVar;
        this.lonVar = lonVar;
        this.is360 = is360;
        this.tolerance = tolerance;
        this.footprint = footprint;
    }
    
    public DatasetConfig() {
        this.tolerance = 0.5;
    }
    
    public String getLatVar() {
        return latVar;
    }
    
    public void setLatVar(String latVar) {
        this.latVar = latVar;
    }
    
    public String getLonVar() {
        return lonVar;
    }
    
    public void setLonVar(String lonVar) {
        this.lonVar = lonVar;
    }
    
    public boolean isIs360() {
        return is360;
    }
    
    public void setIs360(boolean is360) {
        this.is360 = is360;
    }
    
    public double getTolerance() {
        return tolerance;
    }
    
    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }
    
    public FootprintConfig getFootprint() {
        return footprint;
    }
    
    public void setFootprint(FootprintConfig footprint) {
        this.footprint = footprint;
    }
    
}
