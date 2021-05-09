package uk.ac.ebi.ait.filecontentvalidatorservice.reads;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.CommandLineParameters;
import uk.ac.ebi.ait.filecontentvalidatorservice.service.CommandLineParametersBuilder;
import uk.ac.ebi.ait.filecontentvalidatorservice.service.FileContentValidationHandler;
import uk.ac.ebi.ait.filecontentvalidatorservice.utils.ReportTester;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.FileType;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReadsValidationTest {

    private static final Logger log = LoggerFactory.getLogger(ReadsValidationTest.class);
    private static final String FILE_CONTENT_VALIDATION_REPORT_FILENAME = "file-content-validation.report";

    private static final String VALIDATION_RESULT_UUID = UUID.randomUUID().toString();
    private static final String FILE_UUID = UUID.randomUUID().toString();
    private static final String VALIDATION_RESULT2_UUID = UUID.randomUUID().toString();
    private static final String FILE2_UUID = UUID.randomUUID().toString();
    private static String TEST_INITIAL_FILE_PARAMS;
    private static String TEST_INITIAL_FILE2_PARAMS;

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
    public void invalidBAM() throws IOException {
        final String testFilePath = "reads/invalid.bam";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFilePath;
        final CommandLineParameters commandLineParameters = CommandLineParametersBuilder.build(filesParam,
                FileType.BAM.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFilePath)).when(this.validationHandler).getData(anyString());

        assertThat(validationHandler.handleFileContentValidation().getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_ERROR)));

        new ReportTester(validationHandler.getValidationDir()).textInFileReport("invalid.bam", "File contains no valid reads");

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void validBAM() throws IOException {
        final String testFilePath = "reads/valid.bam";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFilePath;
        final CommandLineParameters commandLineParameters =
                CommandLineParametersBuilder.build(filesParam, FileType.BAM.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFilePath)).when(this.validationHandler).getData(anyString());

        assertThat(validationHandler.handleFileContentValidation().getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_SUCCESS)));

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void invalidFastQ() throws IOException {
        final String testFilePath = "reads/invalid.fastq.gz";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFilePath;
        final CommandLineParameters commandLineParameters =
                CommandLineParametersBuilder.build(filesParam, FileType.FASTQ.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFilePath)).when(this.validationHandler).getData(anyString());

        assertThat(validationHandler.handleFileContentValidation().getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_ERROR)));

        new ReportTester(validationHandler.getValidationDir()).textInFileReport("invalid.fastq.gz", "does not match FASTQ regexp");

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void validFastQ() throws IOException {
        final String testFilePath = "reads/valid.fastq.gz";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFilePath;
        final CommandLineParameters commandLineParameters =
                CommandLineParametersBuilder.build(filesParam, FileType.FASTQ.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFilePath)).when(this.validationHandler).getData(testFilePath);

        assertThat(validationHandler.handleFileContentValidation().getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_SUCCESS)));

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void validPairedFastQTwoFiles() throws IOException {
        final String testFile1Path = "reads/valid_paired_1.fastq.gz";
        final String testFile2Path = "reads/valid_paired_2.fastq.gz";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFile1Path + ";"
                + TEST_INITIAL_FILE2_PARAMS + "filePath=" + testFile2Path;
        final CommandLineParameters commandLineParameters =
                CommandLineParametersBuilder.build(filesParam, FileType.FASTQ.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFile1Path)).when(this.validationHandler).getData(testFile1Path);
        doReturn(getResourceFile(testFile2Path)).when(this.validationHandler).getData(testFile2Path);

        final ReadsValidationResponse validationResponse = (ReadsValidationResponse) validationHandler.handleFileContentValidation();
        assertThat(validationResponse.getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_SUCCESS)));

        assertThat(validationResponse.isPaired(), is(equalTo(Boolean.TRUE)));

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void validPairedFastQOneFile() throws IOException {
        final String testFilePath = "reads/valid_paired_single_fastq.gz";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFilePath;
        final CommandLineParameters commandLineParameters =
                CommandLineParametersBuilder.build(filesParam, FileType.FASTQ.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFilePath)).when(this.validationHandler).getData(testFilePath);

        final ReadsValidationResponse validationResponse = (ReadsValidationResponse) validationHandler.handleFileContentValidation();

        assertThat(validationResponse.isPaired(), is(equalTo(Boolean.TRUE)));

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void invalidPairedFastQTwoFiles() throws IOException {
        final String testFile1Path = "reads/invalid_not_paired_1.fastq.gz";
        final String testFile2Path = "reads/invalid_not_paired_2.fastq.gz";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFile1Path + ";"
                + TEST_INITIAL_FILE2_PARAMS + "filePath=" + testFile2Path;
        final CommandLineParameters commandLineParameters =
                CommandLineParametersBuilder.build(filesParam, FileType.FASTQ.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFile1Path)).when(this.validationHandler).getData(testFile1Path);
        doReturn(getResourceFile(testFile2Path)).when(this.validationHandler).getData(testFile2Path);

        assertThat(validationHandler.handleFileContentValidation().getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_ERROR)));

        new ReportTester(validationHandler.getValidationDir()).textInSubmissionReport( "Detected paired fastq submission with less than 20% of paired reads");

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void sameFilePairedFastq() throws IOException {
        final String testFile1Path = "reads/valid.fastq.gz";
        final String testFile2Path = "reads/valid.fastq.gz";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFile1Path + ";"
                + TEST_INITIAL_FILE2_PARAMS + "filePath=" + testFile2Path;
        final CommandLineParameters commandLineParameters =
                CommandLineParametersBuilder.build(filesParam, FileType.FASTQ.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFile1Path)).when(this.validationHandler).getData(testFile1Path);
        doReturn(getResourceFile(testFile2Path)).when(this.validationHandler).getData(testFile2Path);

        assertThat(validationHandler.handleFileContentValidation().getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_ERROR)));

        new ReportTester(validationHandler.getValidationDir()).textInSubmissionReport( "Multiple (1) occurrences of read name");

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void invalidCram() throws IOException {
        final String testFilePath = "reads/invalid.cram";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFilePath;
        final CommandLineParameters commandLineParameters =
                CommandLineParametersBuilder.build(filesParam, FileType.CRAM.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFilePath)).when(this.validationHandler).getData(testFilePath);

        assertThat(validationHandler.handleFileContentValidation().getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_ERROR)));

        new ReportTester(validationHandler.getValidationDir()).textInFileReport( "invalid.cram", "File contains no valid reads");

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void validCram() throws IOException {
        final String testFilePath = "reads/valid.cram";
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + testFilePath;
        final CommandLineParameters commandLineParameters =
                CommandLineParametersBuilder.build(filesParam, FileType.CRAM.toString(), submissionUUID);
        validationHandler.setCommandLineParameters(commandLineParameters);
        doReturn(getResourceFile(testFilePath)).when(this.validationHandler).getData(testFilePath);

        assertThat(validationHandler.handleFileContentValidation().getStatus(), is(equalTo(ValidationResponse.status.VALIDATION_SUCCESS)));

        printReportFileContent(validationHandler.getValidationDir());
    }

    private void printReportFileContent(File reportFilePath) throws IOException {
        System.out.println("Validation Report file content:");
        Files.lines(reportFilePath.toPath()
                .resolve(FILE_CONTENT_VALIDATION_REPORT_FILENAME).toAbsolutePath()).forEach(System.out::println);
    }

    private File getResourceFile(String filename) {
        return new File(
                Objects.requireNonNull(ReadsValidationTest.class.getClassLoader().getResource(filename)).getFile()
        );
    }

    private void deleteReportFileFolderAfterTestExecution() throws IOException {
        Path folderToRemove = validationHandler.getOutputDir().getParentFile().toPath();

        Files.walk(folderToRemove)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
