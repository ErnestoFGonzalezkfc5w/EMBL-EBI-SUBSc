package uk.ac.ebi.ait.filecontentvalidatorservice.utils;

import uk.ac.ebi.ait.filecontentvalidatorservice.exception.FileHandleException;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileUtil {

	public static File getReportFile(File dir, String filename, String suffix) {
		if (dir == null || !dir.isDirectory())
			throw new FileHandleException(FileContentValidatorMessages.CLI_INVALID_REPORT_DIR_ERROR.format(filename));

		return new File(dir, Paths.get(filename).getFileName().toString() + suffix);
	}

	public static boolean emptyDirectory( File dir )
	{
		if (dir == null)
			return false;
	    if( dir.exists() )
	    {
	        File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					emptyDirectory(file);
				} else {
					file.delete();
				}
			}
	    }
	    return dir.listFiles().length == 0;
	}

	public static File createOutputDir(File outputDir, String... dirs) {
		if (outputDir == null) {
			throw new FileHandleException(FileContentValidatorMessages.CLI_MISSING_OUTPUT_DIR_ERROR.text());
		}

		String[] safeDirs = getSafeOutputDirs(dirs);

		Path p;

		try {
			p = Paths.get(outputDir.getPath(), safeDirs);
		} catch (InvalidPathException ex) {
			throw new FileHandleException(FileContentValidatorMessages.CLI_CREATE_DIR_ERROR.format(ex.getInput()));
		}

		File dir = p.toFile();

		if (!dir.exists() && !dir.mkdirs()) {
			throw new FileHandleException(FileContentValidatorMessages.CLI_CREATE_DIR_ERROR.format(dir.getPath()));
		}

		return dir;
	}

	private static String[] getSafeOutputDirs(String... dirs) {
		return Arrays.stream(dirs)
				.map(FileUtil::getSafeOutputDir)
				.toArray(String[]::new);
	}

	private static String getSafeOutputDir(String dir) {
		return dir
				.replaceAll("[^a-zA-Z0-9-_\\.]", "_")
				.replaceAll("_+", "_")
				.replaceAll("^_+(?=[^_])", "")
				.replaceAll("(?<=[^_])_+$", "");
	}

	public static File createTempDir() {
		try {
			File folder = File.createTempFile("test", "test");

			assert(folder.delete());
			assert(folder.mkdirs());

			return folder;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
