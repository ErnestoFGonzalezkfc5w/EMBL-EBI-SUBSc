package uk.ac.ebi.ait.filecontentvalidatorservice.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class FileParameters {

    private String validationResultUUID;
    private String validationResultVersion;
    private String fileUUID;
    private String filePath;
}
