package uk.ac.ebi.ait.filecontentvalidatorservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.CommandLineParameters;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.FileParameters;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.ReportFileConfig;
import uk.ac.ebi.ait.filecontentvalidatorservice.exception.FileContentValidationException;
import uk.ac.ebi.ait.filecontentvalidatorservice.utils.FileContentValidatorMessages;
import uk.ac.ebi.ena.readtools.validator.ReadsValidator;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
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
    CommandLineParameters commandLineParameters;

    private File validationDir;
    private File processDir;

    public FileContentValidationHandler(ReportFileConfig reportFileConfig, CommandLineParameters commandLineParameters) {
        this.validator = new ReadsValidator();
        this.reportFileConfig = reportFileConfig;
        this.commandLineParameters = commandLineParameters;
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
        ReadsManifest manifest = getReadsManifest();
        String submissionUUID = commandLineParameters.getSubmissionUUID();

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

    @NotNull
    private SubmissionFiles<ReadsManifest.FileType> getSubmissionFiles(ReadsManifest.FileType fileType, String... filePaths) {
        SubmissionFiles<ReadsManifest.FileType> submissionFiles = new SubmissionFiles<>();

        for (String filePath : filePaths) {
            SubmissionFile<ReadsManifest.FileType> submissionFile =
                    new SubmissionFile<>(fileType, getData(filePath));
            submissionFiles.add(submissionFile);

        }
        return submissionFiles;
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

    public File getData(String filename) {
        log.debug("file name: {}", filename);
        return new File(filename);
    }

}
