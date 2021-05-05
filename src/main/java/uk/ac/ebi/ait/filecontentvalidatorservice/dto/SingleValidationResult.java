package uk.ac.ebi.ait.filecontentvalidatorservice.dto;

public class SingleValidationResult {
    private ValidationAuthor validationAuthor;

    private SingleValidationResultStatus validationStatus = SingleValidationResultStatus.Pending;

    private String message;

    private String entityUuid;

    public SingleValidationResult() {}

    public SingleValidationResult(ValidationAuthor validationAuthor, String entityUuid) {
        this.validationAuthor = validationAuthor;
        this.entityUuid = entityUuid;
    }

    public ValidationAuthor getValidationAuthor() {
        return validationAuthor;
    }

    public void setValidationAuthor(ValidationAuthor validationAuthor) {
        this.validationAuthor = validationAuthor;
    }

    public SingleValidationResultStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(SingleValidationResultStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEntityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(String entityUuid) {
        this.entityUuid = entityUuid;
    }
}
