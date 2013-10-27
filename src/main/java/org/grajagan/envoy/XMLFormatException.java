package org.grajagan.envoy;

/**
 * This exception is thrown when the XML does not conform with our expectations.
 * 
 */
public class XMLFormatException extends Exception {
    private static final long serialVersionUID = 1L;
    public XMLFormatException(String message) {
        super(message);
    }
}
