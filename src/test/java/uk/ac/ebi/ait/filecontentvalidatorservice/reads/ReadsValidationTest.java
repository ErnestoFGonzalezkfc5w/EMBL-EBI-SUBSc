package uk.ac.ebi.ait.filecontentvalidatorservice.reads;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ait.filecontentvalidatorservice.exception.FileContentValidationException;
import uk.ac.ebi.ait.filecontentvalidatorservice.service.FileContentValidationHandler;
import uk.ac.ebi.ait.filecontentvalidatorservice.utils.ReportTester;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.FileType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReadsValidationTest {

    private static final Logger log = LoggerFactory.getLogger(ReadsValidationTest.class);
    private static final String FILE_CONTENT_VALIDATION_REPORT_FILENAME = "file-content-validation.report";

    private String submissionUUID = UUID.randomUUID().toString();

    @Autowired
    FileContentValidationHandler validationHandler;

    @Test
    public void invalidBAM() throws IOException {
        SubmissionFiles<FileType> submissionFiles = getSubmissionFiles(FileType.BAM, "reads/invalid.bam");
        ReadsManifest readsManifest = getReadsManifest(submissionFiles);

        assertThat(submissionFiles.get(FileType.BAM).size()).isOne();

        assertThatThrownBy(() -> validationHandler.handleFileContentValidation(readsManifest, submissionUUID))
                .isInstanceOf(FileContentValidationException.class)
                .hasMessage("Validation error has happened");

        new ReportTester(validationHandler.getValidationDir()).textInFileReport("invalid.bam", "File contains no valid reads");

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void validBAM() throws IOException {
        SubmissionFiles<FileType> submissionFiles = getSubmissionFiles(FileType.BAM, "reads/valid.bam");
        ReadsManifest readsManifest = getReadsManifest(submissionFiles);

        assertThat(submissionFiles.get().size()).isEqualTo(1);
        assertThat(submissionFiles.get(FileType.BAM).size()).isOne();

        validationHandler.handleFileContentValidation(readsManifest, submissionUUID);

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void invalidFastQ() throws IOException {
        SubmissionFiles<FileType> submissionFiles = getSubmissionFiles(FileType.FASTQ, "reads/invalid.fastq.gz");
        ReadsManifest readsManifest = getReadsManifest(submissionFiles);

        assertThat(submissionFiles.get().size()).isEqualTo(1);
        assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();

        assertThatThrownBy(() -> validationHandler.handleFileContentValidation(readsManifest, submissionUUID))
                .isInstanceOf(FileContentValidationException.class)
                .hasMessage("Validation error has happened");

        new ReportTester(validationHandler.getValidationDir()).textInFileReport("invalid.fastq.gz", "does not match FASTQ regexp");

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void validFastQ() throws IOException {
        SubmissionFiles<FileType> submissionFiles = getSubmissionFiles(FileType.FASTQ, "reads/valid.fastq.gz");
        ReadsManifest readsManifest = getReadsManifest(submissionFiles);

        assertThat(submissionFiles.get().size()).isEqualTo(1);
        assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();

        validationHandler.handleFileContentValidation(readsManifest, submissionUUID);

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void validPairedFastQTwoFiles() throws IOException {
        SubmissionFiles<FileType> submissionFiles = getSubmissionFiles(FileType.FASTQ,
                "reads/valid_paired_1.fastq.gz", "reads/valid_paired_2.fastq.gz");
        ReadsManifest readsManifest = getReadsManifest(submissionFiles);

        assertThat(submissionFiles.get().size()).isEqualTo(2);
        assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);

        validationHandler.handleFileContentValidation(readsManifest, submissionUUID);

        assertThat(validationHandler.getValidationResponse().isPaired());

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void validPairedFastQOneFile() throws IOException {
        SubmissionFiles<FileType> submissionFiles = getSubmissionFiles(FileType.FASTQ,
                "reads/valid_paired_single_fastq.gz");
        ReadsManifest readsManifest = getReadsManifest(submissionFiles);

        assertThat(submissionFiles.get().size()).isEqualTo(1);
        assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();

        validationHandler.handleFileContentValidation(readsManifest, submissionUUID);

        assertThat(validationHandler.getValidationResponse().isPaired());

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void invalidPairedFastQTwoFiles() throws IOException {
        SubmissionFiles<FileType> submissionFiles = getSubmissionFiles(FileType.FASTQ,
                "reads/invalid_not_paired_1.fastq.gz", "reads/invalid_not_paired_2.fastq.gz");
        ReadsManifest readsManifest = getReadsManifest(submissionFiles);

        assertThat(submissionFiles.get().size()).isEqualTo(2);
        assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);

        assertThatThrownBy(() -> validationHandler.handleFileContentValidation(readsManifest, submissionUUID))
                .isInstanceOf(FileContentValidationException.class)
                .hasMessage("Validation error has happened");

        new ReportTester(validationHandler.getValidationDir()).textInSubmissionReport( "Detected paired fastq submission with less than 20% of paired reads");

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void sameFilePairedFastq() throws IOException {
        SubmissionFiles<FileType> submissionFiles = getSubmissionFiles(FileType.FASTQ,
                "reads/valid.fastq.gz", "reads/valid.fastq.gz");
        ReadsManifest readsManifest = getReadsManifest(submissionFiles);

        assertThat(submissionFiles.get().size()).isEqualTo(2);
        assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);

        assertThatThrownBy(() -> validationHandler.handleFileContentValidation(readsManifest, submissionUUID))
                .isInstanceOf(FileContentValidationException.class)
                .hasMessage("Validation error has happened");

        new ReportTester(validationHandler.getValidationDir()).textInSubmissionReport( "Multiple (1) occurrences of read name");

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void invalidCram() throws IOException {
        SubmissionFiles<FileType> submissionFiles = getSubmissionFiles(FileType.CRAM,
                "reads/invalid.cram");
        ReadsManifest readsManifest = getReadsManifest(submissionFiles);

        assertThat(submissionFiles.get().size()).isEqualTo(1);
        assertThat(submissionFiles.get(FileType.CRAM).size()).isOne();

        assertThatThrownBy(() -> validationHandler.handleFileContentValidation(readsManifest, submissionUUID))
                .isInstanceOf(FileContentValidationException.class)
                .hasMessage("Validation error has happened");

        new ReportTester(validationHandler.getValidationDir()).textInFileReport( "invalid.cram", "File contains no valid reads");

        printReportFileContent(validationHandler.getValidationDir());
    }

    @Test
    public void validCram() throws IOException {
        SubmissionFiles<FileType> submissionFiles = getSubmissionFiles(FileType.CRAM,
                "reads/valid.cram");
        ReadsManifest readsManifest = getReadsManifest(submissionFiles);

        assertThat(submissionFiles.get().size()).isEqualTo(1);
        assertThat(submissionFiles.get(FileType.CRAM).size()).isOne();

        validationHandler.handleFileContentValidation(readsManifest, submissionUUID);

        printReportFileContent(validationHandler.getValidationDir());
    }

    @NotNull
    private SubmissionFiles<FileType> getSubmissionFiles(FileType fileType, String... filePaths) {
        SubmissionFiles<FileType> submissionFiles = new SubmissionFiles<>();

        for (String filePath : filePaths) {
            SubmissionFile<FileType> submissionFile =
                    new SubmissionFile<>(fileType, getResourceFile(filePath));
            submissionFiles.add(submissionFile);

        }
        return submissionFiles;
    }

    @NotNull
    private ReadsManifest getReadsManifest(SubmissionFiles<FileType> submissionFiles) {
        ReadsManifest readsManifest = new ReadsManifest();
        readsManifest.setFiles(submissionFiles);
        return readsManifest;
    }

    private void printReportFileContent(File reportFilePath) throws IOException {
        System.out.println("Validation Report file content:");
        Files.lines(reportFilePath.toPath()
                .resolve(FILE_CONTENT_VALIDATION_REPORT_FILENAME).toAbsolutePath()).forEach(System.out::println);
    }

    public static File getResourceFile(String filename) {
        return new File(
                Objects.requireNonNull(ReadsValidationTest.class.getClassLoader().getResource(filename)).getFile()
        );
    }
}
