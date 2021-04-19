package uk.ac.ebi.ait.filecontentvalidatorservice.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportTester {

    private static final String REPORT_FILE = "file-content-validation.report";

    File validationDir;

    public ReportTester(File validationDir) {
        this.validationDir = validationDir;
    }

    public void textInSubmissionReport(String message) {
        textInReport(getSubmissionReportFile(), message);
    }

    private File getFileReport(Path dataFile) {
        return this.validationDir.toPath()
                .resolve(dataFile.getFileName().toString() + ".report").toFile();
    }

    public void textInFileReport(String dataFile, String message) {
        textInReport(getFileReport(Paths.get(dataFile)), message);
    }

    private static void textInReport(File reportFile, String message) {
        assertThat(readFile(reportFile.toPath())).contains(message);
    }

    private static String readFile(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private File getSubmissionReportFile() {
        return Paths.get(this.validationDir.getPath()).resolve(REPORT_FILE).toFile();
    }
}
