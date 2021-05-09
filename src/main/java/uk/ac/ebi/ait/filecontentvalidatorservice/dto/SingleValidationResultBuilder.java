package uk.ac.ebi.ait.filecontentvalidatorservice.dto;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.CommandLineParameters;

@Component
@Getter
@RequiredArgsConstructor
public class SingleValidationResultBuilder {

    @NonNull
    private CommandLineParameters commandLineParams;

    public SingleValidationResult buildSingleValidationResultWithErrorStatus(String message, String fileUUID) {
        SingleValidationResult svr = buildSingleValidationResult(message, fileUUID);
        svr.setValidationStatus(SingleValidationResultStatus.Error);
        return svr;
    }

    public SingleValidationResult buildSingleValidationResultWithWarningStatus(String message, String fileUUID){
        SingleValidationResult svr = buildSingleValidationResult(message, fileUUID);
        svr.setValidationStatus(SingleValidationResultStatus.Warning);
        return svr;
    }

    public SingleValidationResult buildSingleValidationResultWithPassStatus(String fileUUID) {
        SingleValidationResult svr = buildSingleValidationResult("", fileUUID);
        svr.setValidationStatus(SingleValidationResultStatus.Pass);
        return svr;
    }

    private SingleValidationResult buildSingleValidationResult(String errorMessage, String fileUUID) {
        SingleValidationResult singleValidationResult =
                new SingleValidationResult(ValidationAuthor.FileContent, fileUUID);

        if (!StringUtils.isEmpty(errorMessage)) {
            singleValidationResult.setMessage(errorMessage);
        }

        return singleValidationResult;
    }
}
