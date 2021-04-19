package uk.ac.ebi.ait.filecontentvalidatorservice.exception;

public class FileContentValidationException extends RuntimeException {

    private static final String INITIAL_MESSAGE = "Validation error has happened";

    public FileContentValidationException() {
        super(INITIAL_MESSAGE);
    }

    public FileContentValidationException(String message) {
        super(message);
    }

    public FileContentValidationException(Exception ex) {
        super(ex.getMessage(), ex);
    }
}
