package uk.ac.ebi.ait.filecontentvalidatorservice.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

public class ValidationHelper {

    public static File getResourceFile(String filename) {
        return new File(
                Objects.requireNonNull(ValidationHelper.class.getClassLoader().getResource(filename)).getFile()
        );
    }

    public static  void deleteReportFileFolderAfterTestExecution(File outputDir) throws IOException {
        Path folderToRemove = outputDir.getParentFile().toPath();

        Files.walk(folderToRemove)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public static String resourceToAbsolutePath(String filename) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL is = classloader.getResource(filename);
        File file = new File(Objects.requireNonNull(is).getFile());

        return file.getAbsolutePath();
    }
}
