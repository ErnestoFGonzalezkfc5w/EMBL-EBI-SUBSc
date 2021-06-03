package uk.ac.ebi.ait.filecontentvalidatorservice.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.CommandLineParameters;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.FileParameters;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.ReportFileConfig;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.SingleValidationResult;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.SingleValidationResultBuilder;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.SingleValidationResultStatus;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.SingleValidationResultsEnvelope;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.ValidationAuthor;
import uk.ac.ebi.ait.filecontentvalidatorservice.exception.FileContentValidationException;
import uk.ac.ebi.ait.filecontentvalidatorservice.exception.FileHandleException;
import uk.ac.ebi.ait.filecontentvalidatorservice.utils.FileContentValidatorMessages;
import uk.ac.ebi.ait.filecontentvalidatorservice.utils.FileUtil;
import uk.ac.ebi.ena.readtools.validator.ReadsValidator;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import static uk.ac.ebi.ait.filecontentvalidatorservice.service.ErrorMessages.SUBMISSION_FILE_COULD_NOT_BE_FOUND;
import static uk.ac.ebi.ait.filecontentvalidatorservice.service.ErrorMessages.VALIDATION_REPORT_FILE_ERROR;
import static uk.ac.ebi.ait.filecontentvalidatorservice.utils.FileUtil.createOutputDir;

@Service
@Slf4j
public class FileContentValidationHandler {

    private static final String REPORT_FILE = "file-content-validation.report";

    private static final String SUBMISSION_EXCHANGE = "usi-1:submission-exchange";
    private static final String EVENT_VALIDATION_SUCCESS = "validation.success";
    private static final String EVENT_VALIDATION_ERROR = "validation.error";

    ReportFileConfig reportFileConfig;
    ReadsValidator validator;
    ReadsValidationResponse validationResponse;
    CommandLineParameters commandLineParameters;
    ReadsManifest manifest;

    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    private File validationDir;
    private File processDir;

    private Map<String, SubmissionFile> submissionFileByFileUUID = new HashMap<>();

    public FileContentValidationHandler(ReportFileConfig reportFileConfig, CommandLineParameters commandLineParameters,
                                        RabbitMessagingTemplate rabbitMessagingTemplate) {
        this.validator = new ReadsValidator();
        this.reportFileConfig = reportFileConfig;
        this.commandLineParameters = commandLineParameters;
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
    }

    public File getValidationDir() {
        return validationDir;
    }

    public ReadsValidationResponse getValidationResponse() {
        return validationResponse;
    }

    public void setCommandLineParameters(CommandLineParameters commandLineParameters) {
        this.commandLineParameters = commandLineParameters;
    }

    public File getOutputDir() {
        return this.reportFileConfig.getOutputDir();
    }

    public ValidationResponse handleFileContentValidation() {
        manifest = getReadsManifest();
        String submissionUUID = commandLineParameters.getSubmissionUUID();
        String fileUUID = commandLineParameters.getFilesData().stream().map(FileParameters::getFileUUID).collect(Collectors.joining("_"));

        reportFileConfig.setOutputDir(FileUtil.createTempDir(submissionUUID, fileUUID));

        this.validationDir = createSubmissionDir(ReportFileConfig.VALIDATE_DIR, submissionUUID);
        this.processDir = createSubmissionDir(ReportFileConfig.PROCESS_DIR, submissionUUID);

        if(!manifest.getFiles().get().isEmpty()) {
            for (SubmissionFile<ReadsManifest.FileType> subFile : (manifest.getFiles().get())) {
                subFile.setReportFile(
                        Paths.get(this.validationDir.getPath())
                                .resolve(subFile.getFile().getName() + ".report").toFile());
            }
        }

        final File submissionReportFile = getSubmissionReportFile();
        log.info(submissionReportFile.getAbsolutePath());
        manifest.setReportFile(submissionReportFile);
        manifest.setProcessDir(this.processDir);

        log.debug("Before validation");

        try {
            validationResponse = validator.validate(manifest);
        } catch (RuntimeException ex) {
            throw new FileContentValidationException(ex);
        }

        log.info("Validation response: {}", validationResponse.getStatus());

        return validationResponse;
    }

    public void sendValidationMessagesToAggregator(ValidationResponse validationResponse) {
        commandLineParameters.getFilesData().forEach(fileParameters -> {
            List<SingleValidationResult> validationResultByFileUUID =
                    createValidationResultByFileUUID(validationResponse, fileParameters.getFileUUID());
            SingleValidationResultsEnvelope singleValidationResultsEnvelope =
                    generateSingleValidationResultsEnvelope(validationResultByFileUUID,
                            Integer.parseInt(fileParameters.getValidationResultVersion()),
                            fileParameters.getValidationResultUUID());
            sendValidationMessageToAggregator(singleValidationResultsEnvelope);
        });
    }

    public List<SingleValidationResult> createValidationResultByFileUUID(ValidationResponse validationResponse, String fileUUID) {
        List<SingleValidationResult> validationResults = new ArrayList<>();
        if (validationResponse.getStatus().equals(ValidationResponse.status.VALIDATION_SUCCESS)) {
            validationResults.add(SingleValidationResultBuilder.buildSingleValidationResultWithPassStatus(fileUUID));
        } else {
            validationResults.addAll(parseResultFiles(fileUUID));
        }

        return validationResults;
    }

    private List<SingleValidationResult> parseResultFiles(String fileUUID) {
        List<SingleValidationResult> validationResults = new ArrayList<>();

        final SubmissionFile submissionFile = submissionFileByFileUUID.get(fileUUID);
        if (submissionFile == null) {
            throw new FileContentValidationException(String.format(SUBMISSION_FILE_COULD_NOT_BE_FOUND, fileUUID));
        }

        validationResults.addAll(parseResultFile(fileUUID, submissionFile.getReportFile()));

        validationResults.addAll(parseCommonResultFile(fileUUID));

        return validationResults;
    }

    private List<SingleValidationResult> parseCommonResultFile(String fileUUID) {
        List<SingleValidationResult> validationResults = new ArrayList<>();
        validationResults.addAll(parseResultFile(fileUUID, getSubmissionReportFile()));

        return validationResults;
    }

    private List<SingleValidationResult> parseResultFile(String fileUUID, File reportFile) {
        List<SingleValidationResult> validationResults = new ArrayList<>();
        try (Scanner scanner = new Scanner(new BufferedReader(new FileReader(reportFile)))) {
            while (scanner.hasNext()) {
                String message = scanner.nextLine();

                if (message.startsWith("ERROR: ")) {
                    message = message.replace("ERROR: ", "");
                    validationResults.add(
                            SingleValidationResultBuilder.buildSingleValidationResultWithErrorStatus(message, fileUUID));
                }
            }
        } catch (IOException ex) {
            throw new FileHandleException(String.format(VALIDATION_REPORT_FILE_ERROR, fileUUID));
        }

        return validationResults;
    }

    @NotNull
    private SubmissionFiles<ReadsManifest.FileType> getSubmissionFiles(ReadsManifest.FileType fileType, String... filePaths) {
        SubmissionFiles<ReadsManifest.FileType> submissionFiles = new SubmissionFiles<>();

        for (String filePath : filePaths) {
            SubmissionFile<ReadsManifest.FileType> submissionFile =
                    new SubmissionFile<>(fileType, getData(filePath));
            submissionFiles.add(submissionFile);

            getFileUUIDByFilePath(filePath).ifPresent(fileUUID ->
                submissionFileByFileUUID.put(fileUUID, submissionFile)
            );
        }
        return submissionFiles;
    }

    private Optional<String> getFileUUIDByFilePath(String filePath) {
        return commandLineParameters.getFilesData().stream()
                .filter(fileParameter -> fileParameter.getFilePath().equals(filePath))
                .map(FileParameters::getFileUUID)
                .findFirst();
    }

    @NotNull
    private ReadsManifest getReadsManifest() {
        final ReadsManifest.FileType fileType = ReadsManifest.FileType.valueOf(commandLineParameters.getFileType());
        final String[] filePath = commandLineParameters.getFilesData().stream()
                .map(FileParameters::getFilePath)
                .toArray(String[]::new);

        SubmissionFiles<ReadsManifest.FileType> submissionFiles = getSubmissionFiles(fileType, filePath);
        ReadsManifest readsManifest = new ReadsManifest();
        readsManifest.setFiles(submissionFiles);

        return readsManifest;
    }


    private File createSubmissionDir(String dir, String submissionUUID) {
        if (StringUtils.isBlank(submissionUUID)) {
            throw new FileContentValidationException(
                    FileContentValidatorMessages.EXECUTOR_INIT_ERROR.format("Missing submission's UUID."));
        }

        return createOutputDir(reportFileConfig.getOutputDir(), reportFileConfig.getContextType(), dir);
    }

    private File getSubmissionReportFile() {
        return Paths.get(this.validationDir.getPath()).resolve(REPORT_FILE).toFile();
    }

    public File getData(String filename) {
        log.debug("file name: {}", filename);
        return new File(filename);
    }

    private SingleValidationResultsEnvelope generateSingleValidationResultsEnvelope(
            List<SingleValidationResult> singleValidationResults, int validationResultVersion,
            String validationResultUUID) {
        return new SingleValidationResultsEnvelope(
                singleValidationResults,
                validationResultVersion,
                validationResultUUID,
                ValidationAuthor.FileContent
        );
    }

    private void sendValidationMessageToAggregator(SingleValidationResultsEnvelope envelope) {
        List<SingleValidationResult> errorResults =
                envelope.getSingleValidationResults().stream()
                        .filter(svr -> svr.getValidationStatus().equals(SingleValidationResultStatus.Error))
                        .collect(Collectors.toList());

        if (errorResults.size() > 0) {
            rabbitMessagingTemplate.convertAndSend(SUBMISSION_EXCHANGE, EVENT_VALIDATION_ERROR, envelope);
        } else {
            rabbitMessagingTemplate.convertAndSend(SUBMISSION_EXCHANGE, EVENT_VALIDATION_SUCCESS, envelope);
        }
    }
}
