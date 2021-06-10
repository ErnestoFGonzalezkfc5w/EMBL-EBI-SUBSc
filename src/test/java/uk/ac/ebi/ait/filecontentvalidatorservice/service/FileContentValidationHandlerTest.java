package uk.ac.ebi.ait.filecontentvalidatorservice.service;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.CommandLineParameters;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.SingleValidationResult;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.SingleValidationResultStatus;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static uk.ac.ebi.ait.filecontentvalidatorservice.service.ValidationHelper.deleteReportFileFolderAfterTestExecution;
import static uk.ac.ebi.ait.filecontentvalidatorservice.service.ValidationHelper.getResourceFile;
import static uk.ac.ebi.ait.filecontentvalidatorservice.service.ValidationHelper.resourceToAbsolutePath;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FileContentValidationHandlerTest {

    private static final String VALIDATION_RESULT_UUID = UUID.randomUUID().toString();
    private static final String FILE_UUID = UUID.randomUUID().toString();
    private static final String VALIDATION_RESULT2_UUID = UUID.randomUUID().toString();
    private static final String FILE2_UUID = UUID.randomUUID().toString();
    private static String TEST_INITIAL_FILE_PARAMS;
    private static String TEST_INITIAL_FILE2_PARAMS;
    private static final List<String> REPORT_FILES = Arrays.asList("reportfiles/invalid_not_paired_1.fastq.gz.report",
                                                                    "reportfiles/invalid_not_paired_2.fastq.gz.report");

    private String submissionUUID = UUID.randomUUID().toString();

    @SpyBean
    FileContentValidationHandler validationHandler;

    @BeforeClass
    public static void testInit() {
        TEST_INITIAL_FILE_PARAMS = "validationResultUUID=" + VALIDATION_RESULT_UUID + ","
                + "validationResultVersion=0,"
                + "fileUUID=" + FILE_UUID + ",";
        TEST_INITIAL_FILE2_PARAMS = "validationResultUUID=" + VALIDATION_RESULT2_UUID + ","
                + "validationResultVersion=0,"
                + "fileUUID=" + FILE2_UUID + ",";
    }

    @After
    public void tearDown() throws IOException {
        deleteReportFileFolderAfterTestExecution();
    }

    @Test
    public void whenSingleFileValidationFailed_ThenReturnFailedValidationResult() {
        final String testFilePath = "reads/invalid.bam";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFilePath;
        final CommandLineParameters commandLineParameters = CommandLineParametersBuilder.build(filesParam,
                ReadsManifest.FileType.BAM.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFilePath)).when(this.validationHandler).getData(anyString());

        final ValidationResponse validationResponse = validationHandler.handleFileContentValidation();

        int reportFileIndex = 0;
        for (SubmissionFile<ReadsManifest.FileType> submissionFile: validationHandler.manifest.getFiles().get()) {
            submissionFile.setReportFile(new File(resourceToAbsolutePath(REPORT_FILES.get(reportFileIndex++))));
        }

        assertThat(validationResponse.getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_ERROR)));

        String expectedValidationResultMessage1 = "This is an error message";
        String expectedValidationResultMessageCommon = "File contains no valid reads";
        final List<SingleValidationResult> validationResult =
                validationHandler.createValidationResultByFileUUID(validationResponse, FILE_UUID);

        assertThat(validationResult.size(), is(equalTo(2)));

        List<String> actualMessages = validationResult.stream()
                .map(SingleValidationResult::getMessage).collect(Collectors.toList());
        List<SingleValidationResultStatus> statuses = validationResult.stream()
                .map(SingleValidationResult::getValidationStatus).collect(Collectors.toList());

        assertThat(statuses.size(), is(equalTo(2)));
        assertThat(statuses, hasItem(SingleValidationResultStatus.Error));
        assertThat(actualMessages, hasItem(expectedValidationResultMessage1));
        assertThat(actualMessages, hasItem(containsString(expectedValidationResultMessageCommon)));
    }

    @Test
    public void whenPairedFileValidationFailed_ThenReturnFailedValidationResult() {
        final String testFile1Path = "reads/invalid_not_paired_1.fastq.gz";
        final String testFile2Path = "reads/invalid_not_paired_2.fastq.gz";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFile1Path + ";"
                + TEST_INITIAL_FILE2_PARAMS + "filePath=" + testFile2Path;
        final CommandLineParameters commandLineParameters =
                CommandLineParametersBuilder.build(filesParam, ReadsManifest.FileType.FASTQ.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFile1Path)).when(this.validationHandler).getData(testFile1Path);
        doReturn(getResourceFile(testFile2Path)).when(this.validationHandler).getData(testFile2Path);

        final ValidationResponse validationResponse = validationHandler.handleFileContentValidation();

        int reportFileIndex = 0;
        for (SubmissionFile<ReadsManifest.FileType> submissionFile: validationHandler.manifest.getFiles().get()) {
            submissionFile.setReportFile(new File(resourceToAbsolutePath(REPORT_FILES.get(reportFileIndex++))));
        }

        assertThat(validationResponse.getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_ERROR)));

        String expectedValidationResultMessageFile1 = "This is an error message";
        String expectedValidationResultMessageFile2 = "This is another error message";
        String expectedValidationResultMessageCommon = "Detected paired fastq submission with less than 20% of paired reads";

        final List<SingleValidationResult> validationResultForFile1 =
                validationHandler.createValidationResultByFileUUID(validationResponse, FILE_UUID);
        List<String> actualMessagesForFile1 = validationResultForFile1.stream()
                .map(SingleValidationResult::getMessage).collect(Collectors.toList());
        List<SingleValidationResultStatus> statusesForFile1 = validationResultForFile1.stream()
                .map(SingleValidationResult::getValidationStatus).collect(Collectors.toList());

        assertThat(validationResultForFile1.size(), is(equalTo(2)));
        assertThat(statusesForFile1.size(), is(equalTo(2)));
        assertThat(actualMessagesForFile1, hasItem(containsString(expectedValidationResultMessageFile1)));
        assertThat(actualMessagesForFile1, hasItem(containsString(expectedValidationResultMessageCommon)));

        final List<SingleValidationResult> validationResultForFile2 =
                validationHandler.createValidationResultByFileUUID(validationResponse, FILE2_UUID);
        List<String> actualMessagesForFile2 = validationResultForFile2.stream()
                .map(SingleValidationResult::getMessage).collect(Collectors.toList());
        List<SingleValidationResultStatus> statusesForFile2 = validationResultForFile2.stream()
                .map(SingleValidationResult::getValidationStatus).collect(Collectors.toList());

        assertThat(validationResultForFile2.size(), is(equalTo(3)));
        assertThat(statusesForFile2.size(), is(equalTo(3)));
        assertThat(actualMessagesForFile2, hasItem(containsString(expectedValidationResultMessageFile2)));
        assertThat(actualMessagesForFile2, hasItem(containsString(expectedValidationResultMessageCommon)));
    }

    @Test
    public void whenValidationPassed_ThenReturnPassedValidationResult() {
        final String testFilePath = "reads/valid.bam";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFilePath;
        final CommandLineParameters commandLineParameters = CommandLineParametersBuilder.build(filesParam,
                ReadsManifest.FileType.BAM.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFilePath)).when(this.validationHandler).getData(anyString());

        final ValidationResponse validationResponse = validationHandler.handleFileContentValidation();

        assertThat(validationResponse.getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_SUCCESS)));

        final List<SingleValidationResult> validationResult =
                validationHandler.createValidationResultByFileUUID(validationResponse, FILE_UUID);

        assertThat(validationResult.get(0).getValidationStatus(), is(equalTo(SingleValidationResultStatus.Pass)));
    }

    @Test
    public void whenValidationExecuted2Times_Then2SeparateReportFoldersCreated() {
        final String testFilePath = "reads/valid.bam";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFilePath;
        final CommandLineParameters commandLineParameters = CommandLineParametersBuilder.build(filesParam,
                ReadsManifest.FileType.BAM.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFilePath)).when(this.validationHandler).getData(anyString());

        validationHandler.handleFileContentValidation();
        validationHandler.handleFileContentValidation();

        assertThat(validationHandler.getOutputDir().getParentFile().list().length, is(equalTo(2)));
    }
}
