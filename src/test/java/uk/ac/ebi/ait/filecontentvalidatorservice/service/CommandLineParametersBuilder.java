package uk.ac.ebi.ait.filecontentvalidatorservice.service;

import uk.ac.ebi.ait.filecontentvalidatorservice.config.CommandLineParameters;

public class CommandLineParametersBuilder {

    public static CommandLineParameters build(String files, String fileType, String submissionUUID) {
        CommandLineParameters commandLineParams = new CommandLineParameters();
        commandLineParams.setCommandLineFileParameters(files);
        commandLineParams.setFileType(fileType);
        commandLineParams.setSubmissionUUID(submissionUUID);

        return commandLineParams;
    }
}
