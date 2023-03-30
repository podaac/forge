package gov.nasa.podaac.forge;

public class FootprintException extends Exception {
    public FootprintException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public FootprintException(String message) {
        super(message);
    }
}
