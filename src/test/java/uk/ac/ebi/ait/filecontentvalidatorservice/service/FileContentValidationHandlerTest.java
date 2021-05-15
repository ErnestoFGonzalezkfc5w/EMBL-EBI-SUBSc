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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
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
        deleteReportFileFolderAfterTestExecution(validationHandler.getOutputDir());
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
        assertThat(validationResult.get(0).getValidationStatus(), is(equalTo(SingleValidationResultStatus.Error)));
        assertThat(validationResult.get(0).getMessage(), is(containsString(expectedValidationResultMessage1)));
        assertThat(validationResult.get(1).getValidationStatus(), is(equalTo(SingleValidationResultStatus.Error)));
        assertThat(validationResult.get(1).getMessage(), is(containsString(expectedValidationResultMessageCommon)));
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

        assertThat(validationResultForFile1.size(), is(equalTo(2)));
        assertThat(validationResultForFile1.get(0).getValidationStatus(), is(equalTo(SingleValidationResultStatus.Error)));
        assertThat(validationResultForFile1.get(0).getMessage(), is(containsString(expectedValidationResultMessageFile1)));
        assertThat(validationResultForFile1.get(1).getValidationStatus(), is(equalTo(SingleValidationResultStatus.Error)));
        assertThat(validationResultForFile1.get(1).getMessage(), is(containsString(expectedValidationResultMessageCommon)));

        final List<SingleValidationResult> validationResultForFile2 =
                validationHandler.createValidationResultByFileUUID(validationResponse, FILE2_UUID);

        assertThat(validationResultForFile2.size(), is(equalTo(2)));
        assertThat(validationResultForFile2.get(0).getValidationStatus(), is(equalTo(SingleValidationResultStatus.Error)));
        assertThat(validationResultForFile2.get(0).getMessage(), is(containsString(expectedValidationResultMessageFile2)));
        assertThat(validationResultForFile2.get(1).getValidationStatus(), is(equalTo(SingleValidationResultStatus.Error)));
        assertThat(validationResultForFile2.get(1).getMessage(), is(containsString(expectedValidationResultMessageCommon)));
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
}
