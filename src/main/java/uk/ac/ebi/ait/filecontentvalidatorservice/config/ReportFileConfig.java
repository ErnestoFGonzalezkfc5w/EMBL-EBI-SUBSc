package uk.ac.ebi.ait.filecontentvalidatorservice.config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.File;

@Data
@Component
public class ReportFileConfig {
    public static final String VALIDATE_DIR = "validate";
    public static final String PROCESS_DIR = "process";

    private File outputDir;
    private File inputDir = new File( "." );
    private String contextType = "reads";
}
