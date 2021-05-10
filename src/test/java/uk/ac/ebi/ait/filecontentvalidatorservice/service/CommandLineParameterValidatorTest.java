package uk.ac.ebi.ait.filecontentvalidatorservice.service;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ait.filecontentvalidatorservice.config.CommandLineParameters;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.SingleValidationResult;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.SingleValidationResultStatus;
import uk.ac.ebi.ait.filecontentvalidatorservice.dto.ValidationAuthor;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.Assert.assertThat;
import static uk.ac.ebi.ait.filecontentvalidatorservice.service.ValidationHelper.resourceToAbsolutePath;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CommandLineParameterValidatorTest {

    private static final String TEST_FILE_INVALID_PATH = "/invalid/path";
    private static final String TEST_FILE_FOR_FILE_CONTENT_VALIDATION = "test_file_for_file_content_validation.txt";
    private static final String TEST_FILE_PATH = resourceToAbsolutePath(TEST_FILE_FOR_FILE_CONTENT_VALIDATION);
    private static final String VALIDATION_RESULT_UUID = "112233-aabbcc-223344";
    private static final String FILE_UUID = "9999-aabbcc-223344";
    private static final String FILE_TYPE = "fastQ";
    private static final String NOT_SUPPORTED_FILE_TYPE = "qseq";
    private static final String SUBMISSION_UUID = UUID.randomUUID().toString();

    private static String TEST_INITIAL_FILE_PARAMS;

    @Autowired
    private CommandLineParameterValidator commandLineParameterValidator;

    @BeforeClass
    public static void testInit() {
        TEST_INITIAL_FILE_PARAMS = "validationResultUUID=" + VALIDATION_RESULT_UUID + ","
                + "validationResultVersion=0,"
                + "fileUUID=" + FILE_UUID + ",";
    }

    @Test
    public void whenFileDoesNotExistInTheProvidedPath_ThenThrowsFileNotFoundValidationError() {
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + TEST_FILE_INVALID_PATH;
        CommandLineParameters commandLineParams =
                CommandLineParametersBuilder.build(filesParam, FILE_TYPE, SUBMISSION_UUID);
        String expectedValidationError = String.format(ErrorMessages.FILE_NOT_FOUND_BY_TARGET_PATH, TEST_FILE_INVALID_PATH);

        List<SingleValidationResult> parameterErrors = commandLineParameterValidator.validateParameters(commandLineParams);
        final SingleValidationResult singleValidationResult = parameterErrors.get(0);

        assertThat(parameterErrors, not(emptyIterable()));
        assertThat(singleValidationResult.getValidationAuthor(), is(equalTo(ValidationAuthor.FileContent)));
        assertThat(singleValidationResult.getValidationStatus(), is(equalTo(SingleValidationResultStatus.Error)));
        assertThat(singleValidationResult.getMessage(), is(equalTo(expectedValidationError)));
    }

    @Test
    public void whenUnsupportedFileTypeProvided_ThenThrowsNotSupportedFileTypeException() {
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + TEST_FILE_PATH;
        CommandLineParameters commandLineParams =
                CommandLineParametersBuilder.build(filesParam, NOT_SUPPORTED_FILE_TYPE, SUBMISSION_UUID);

        String expectedValidationError = String.format(ErrorMessages.FILE_TYPE_NOT_SUPPORTED, NOT_SUPPORTED_FILE_TYPE);

        List<SingleValidationResult> parameterErrors = commandLineParameterValidator.validateParameters(commandLineParams);
        final SingleValidationResult singleValidationResult = parameterErrors.get(0);

        assertThat(parameterErrors, not(emptyIterable()));
        assertThat(singleValidationResult.getValidationAuthor(), is(equalTo(ValidationAuthor.FileContent)));
        assertThat(singleValidationResult.getValidationStatus(), is(equalTo(SingleValidationResultStatus.Error)));
        assertThat(singleValidationResult.getMessage(), is(equalTo(expectedValidationError)));
    }

    @Test
    public void whenParametersValid_ThenNoErrors() {
        String filesParam = TEST_INITIAL_FILE_PARAMS + "filePath=" + TEST_FILE_PATH;
        CommandLineParameters commandLineParams =
                CommandLineParametersBuilder.build(filesParam, FILE_TYPE, SUBMISSION_UUID);

        assertThat(commandLineParameterValidator.validateParameters(commandLineParams), emptyIterable());
    }
}
