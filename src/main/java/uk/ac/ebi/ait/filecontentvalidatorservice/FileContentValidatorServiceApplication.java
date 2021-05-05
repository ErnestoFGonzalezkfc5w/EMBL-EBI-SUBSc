package uk.ac.ebi.ait.filecontentvalidatorservice;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import uk.ac.ebi.ait.filecontentvalidatorservice.service.FileContentValidationHandler;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class FileContentValidatorServiceApplication implements ApplicationRunner {

	@NonNull
	private FileContentValidationHandler fileContentValidationHandler;

	private static final String FILE_PATH_OPTION = "fileContentValidator.files";

	public static void main(String[] args) {
		final ConfigurableApplicationContext ctx = SpringApplication.run(FileContentValidatorServiceApplication.class, args);

		SpringApplication.exit(ctx, () -> 0);
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("FileContentValidatorApplication started executing.");

		if (!args.getOptionNames().isEmpty()) {
			log.info("File content validation started for file(s): {}", args.getOptionValues(FILE_PATH_OPTION));

			fileContentValidationHandler.handleFileContentValidation();
		}
	}
}
