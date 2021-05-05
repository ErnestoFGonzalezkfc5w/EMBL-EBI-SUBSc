package uk.ac.ebi.ait.filecontentvalidatorservice.dto;

public enum SingleValidationResultStatus {
    Pending,
    Pass,
    Error,
    Warning;

    private SingleValidationResultStatus() {
    }
}
