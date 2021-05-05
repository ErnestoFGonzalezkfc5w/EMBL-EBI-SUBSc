package uk.ac.ebi.ait.filecontentvalidatorservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.ait.filecontentvalidatorservice.service.FileType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@Data
public class CommandLineParameters {

    @Value("${fileContentValidator.files:''}")
    private String commandLineFileParameters;

    @Value("${fileContentValidator.fileType:''}")
    private String fileType;
    @Value("${fileContentValidator.submissionUUID:''}")
    private String submissionUUID;

    final ObjectMapper mapper = new ObjectMapper();

    public FileType getFileTypeEnum(){
        return FileType.valueOf(fileType);
    }

    public List<FileParameters> getFilesData() {
        List<FileParameters> filesData = new ArrayList<>();

        Arrays.asList(commandLineFileParameters.split(";")).forEach(fileData -> {
            Map<String, String> paramMap = Arrays.stream(fileData.split(","))
                    .map(s -> s.split("=", 2))
                    .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
            filesData.add(mapper.convertValue(paramMap, FileParameters.class));
        });

        return filesData;
    }
}
