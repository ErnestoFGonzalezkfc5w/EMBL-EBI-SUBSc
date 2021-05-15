package uk.ac.ebi.ait.filecontentvalidatorservice.dto;

import java.util.List;

/**
 * This is a Data Transfer Object transferring data from a {@code Validator} to the {@code validator-aggregator} service.
 */
public class SingleValidationResultsEnvelope {

    private List<SingleValidationResult> singleValidationResults;
    private int validationResultVersion;
    private String validationResultUUID;
    private ValidationAuthor validationAuthor;

    public SingleValidationResultsEnvelope() {}

    /**
     * Constructor of the {@code SingleValidationResultsEnvelope} object.
     *
     * @param singleValidationResults validation result(s) returned from a specific validator
     * @param validationResultVersion the version of the {@code uk.ac.ebi.subs.validator.data.ValidationResult} the validation relates to
     * @param validationResultUUID the UUID of the {@code uk.ac.ebi.subs.validator.data.ValidationResult} the validation relates to
     * @param validationAuthor the author of the validation
     */
    public SingleValidationResultsEnvelope(List<SingleValidationResult> singleValidationResults, int validationResultVersion,
                                           String validationResultUUID, ValidationAuthor validationAuthor) {
        this.singleValidationResults = singleValidationResults;
        this.validationResultVersion = validationResultVersion;
        this.validationResultUUID = validationResultUUID;
        this.validationAuthor = validationAuthor;
    }

    public List<SingleValidationResult> getSingleValidationResults() {
        return singleValidationResults;
    }

    public void setSingleValidationResults(List<SingleValidationResult> singleValidationResults) {
        this.singleValidationResults = singleValidationResults;
    }

    public int getValidationResultVersion() {
        return validationResultVersion;
    }

    public void setValidationResultVersion(int validationResultVersion) {
        this.validationResultVersion = validationResultVersion;
    }

    public String getValidationResultUUID() {
        return validationResultUUID;
    }

    public void setValidationResultUUID(String validationResultUUID) {
        this.validationResultUUID = validationResultUUID;
    }

    public ValidationAuthor getValidationAuthor() {
        return validationAuthor;
    }

    public void setValidationAuthor(ValidationAuthor validationAuthor) {
        this.validationAuthor = validationAuthor;
    }
}
