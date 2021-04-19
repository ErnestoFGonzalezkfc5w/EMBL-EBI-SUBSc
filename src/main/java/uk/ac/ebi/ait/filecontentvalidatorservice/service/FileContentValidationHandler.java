package uk.ac.ebi.ait.filecontentvalidatorservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.ReportFileConfig;
import uk.ac.ebi.ait.filecontentvalidatorservice.exception.FileContentValidationException;
import uk.ac.ebi.ait.filecontentvalidatorservice.utils.FileContentValidatorMessages;
import uk.ac.ebi.ena.readtools.validator.ReadsValidator;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

import java.io.File;
import java.nio.file.Paths;

import static uk.ac.ebi.ait.filecontentvalidatorservice.utils.FileUtil.createOutputDir;
import static uk.ac.ebi.ait.filecontentvalidatorservice.utils.FileUtil.emptyDirectory;

@Service
@Slf4j
public class FileContentValidationHandler {

    private static final String REPORT_FILE = "file-content-validation.report";

    ReportFileConfig reportFileConfig;
    ReadsValidator validator;
    ReadsValidationResponse validationResponse;

    private File validationDir;
    private File processDir;

    public FileContentValidationHandler(ReportFileConfig reportFileConfig) {
        this.validator = new ReadsValidator();
        this.reportFileConfig = reportFileConfig;
    }

    public File getValidationDir() {
        return validationDir;
    }

    public ReadsValidationResponse getValidationResponse() {
        return validationResponse;
    }

    public void handleFileContentValidation(ReadsManifest manifest, String submissionUUID) {
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

        try {
            validationResponse = validator.validate(manifest);
        } catch (RuntimeException ex) {
            throw new FileContentValidationException(ex);
        }

        if (validationResponse != null
                && validationResponse.getStatus() == ValidationResponse.status.VALIDATION_ERROR) {
            throw new FileContentValidationException();
        }
    }

    private File createSubmissionDir(String dir, String submissionUUID) {
        if (StringUtils.isBlank(submissionUUID)) {
            throw new FileContentValidationException(
                    FileContentValidatorMessages.EXECUTOR_INIT_ERROR.format("Missing submission's UUID."));
        }
        File newDir =
                createOutputDir(reportFileConfig.getOutputDir(), reportFileConfig.getContextType(), submissionUUID, dir);
        if (!emptyDirectory(newDir)) {
            throw new FileContentValidationException(
                    FileContentValidatorMessages.EXECUTOR_EMPTY_DIRECTORY_ERROR.format(newDir));
        }
        return newDir;
    }

    private File getSubmissionReportFile() {
        return Paths.get(this.validationDir.getPath()).resolve(REPORT_FILE).toFile();
    }
}
