package uk.ac.ebi.ait.filecontentvalidatorservice.utils;

import uk.ac.ebi.ena.webin.cli.validator.message.source.MessageFormatSource;

public enum FileContentValidatorMessages implements MessageFormatSource {

    CLI_INVALID_REPORT_DIR_ERROR("invalid report directory: {0}"),
    CLI_MISSING_OUTPUT_DIR_ERROR("Missing output directory."),
    CLI_CREATE_DIR_ERROR("Unable to create directory: {0}"),

    EXECUTOR_INIT_ERROR("Failed to initialise validator. {0}"),
    EXECUTOR_EMPTY_DIRECTORY_ERROR("Unable to empty directory {0}");

    private final String text;

    FileContentValidatorMessages(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
