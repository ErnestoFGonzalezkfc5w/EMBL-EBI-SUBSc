package uk.ac.ebi.ait.filecontentvalidatorservice.exception;

public class FileHandleException extends RuntimeException {

    public FileHandleException(String message) {
        super(message);
    }

    public FileHandleException(String message, Exception originalCause) {
        super(message, originalCause);
    }
}
