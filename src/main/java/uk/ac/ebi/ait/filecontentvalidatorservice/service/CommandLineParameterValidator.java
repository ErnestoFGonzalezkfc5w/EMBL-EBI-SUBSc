package uk.ac.ebi.ait.filecontentvalidatorservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.CommandLineParameters;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.FileParameters;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.SingleValidationResult;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.SingleValidationResultBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommandLineParameterValidator {

    public List<SingleValidationResult> validateParameters(CommandLineParameters commandLineParams) {

        List<SingleValidationResult> validationErrors = new ArrayList<>(validateFileExistence(commandLineParams));

        validateFileType(commandLineParams).ifPresent(validationErrors::add);

        return validationErrors;
    }

    private List<SingleValidationResult> validateFileExistence(CommandLineParameters commandLineParams) {
        List<SingleValidationResult> fileExistenceErrors = new ArrayList<>();
        commandLineParams.getFilesData().forEach(fileParams -> {
            String filePath = fileParams.getFilePath();
            File file = new File(filePath);
            if (!file.exists() || file.isDirectory()) {
                fileExistenceErrors.add(SingleValidationResultBuilder.buildSingleValidationResultWithErrorStatus(
                        String.format(ErrorMessages.FILE_NOT_FOUND_BY_TARGET_PATH, filePath), fileParams.getFileUUID()));
            }
        });

        return fileExistenceErrors;
    }

    private Optional<SingleValidationResult> validateFileType(CommandLineParameters commandLineParams) {
        String fileType = commandLineParams.getFileType();
        String fileUUIDs = commandLineParams.getFilesData().stream()
                .map(FileParameters::getFileUUID)
                .collect(Collectors.joining(", "));
        if (!FileType.isSupported(fileType)) {
            return Optional.of(
                    SingleValidationResultBuilder.buildSingleValidationResultWithErrorStatus(
                            String.format(ErrorMessages.FILE_TYPE_NOT_SUPPORTED, fileType), fileUUIDs));
        }

        return Optional.empty();
    }
}
