package uk.ac.ebi.ait.filecontentvalidatorservice.dto;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SingleValidationResultBuilder {

    public static SingleValidationResult buildSingleValidationResultWithErrorStatus(String message, String fileUUID) {
        SingleValidationResult svr = buildSingleValidationResult(message, fileUUID);
        svr.setValidationStatus(SingleValidationResultStatus.Error);
        return svr;
    }

    public static SingleValidationResult buildSingleValidationResultWithWarningStatus(String message, String fileUUID){
        SingleValidationResult svr = buildSingleValidationResult(message, fileUUID);
        svr.setValidationStatus(SingleValidationResultStatus.Warning);
        return svr;
    }

    public static SingleValidationResult buildSingleValidationResultWithPassStatus(String fileUUID) {
        SingleValidationResult svr = buildSingleValidationResult("", fileUUID);
        svr.setValidationStatus(SingleValidationResultStatus.Pass);
        return svr;
    }

    private static SingleValidationResult buildSingleValidationResult(String errorMessage, String fileUUID) {
        SingleValidationResult singleValidationResult =
                new SingleValidationResult(ValidationAuthor.FileContent, fileUUID);

        if (!StringUtils.isEmpty(errorMessage)) {
            singleValidationResult.setMessage(errorMessage);
        }

        return singleValidationResult;
    }
}
