package uk.ac.ebi.ait.filecontentvalidatorservice.config;

import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ait.filecontentvalidatorservice.utils.FileUtil;

import java.io.File;

@Data
@Component
public class ReportFileConfig {
    public static final String VALIDATE_DIR = "validate";
    public static final String PROCESS_DIR = "process";

    private File outputDir = FileUtil.createTempDir();
    private File inputDir = new File( "." );
    private String contextType = "reads";
}
