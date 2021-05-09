package uk.ac.ebi.ait.filecontentvalidatorservice.service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum FileType {
    FASTQ, BAM, CRAM, VCF;

    private static List<String> SUPPORTED_TYPES = Arrays.asList(
            FileType.FASTQ.name(),
            FileType.VCF.name(),
            FileType.BAM.name(),
            FileType.CRAM.name()
    );

    private static final Map<String, FileType> nameToValueMap = new HashMap<>();

    static {
        for (FileType value : EnumSet.allOf(FileType.class)) {
            nameToValueMap.put(value.name(), value);
        }
    }

    public static boolean isSupported(String name) {
        return SUPPORTED_TYPES.contains(name.toUpperCase());
    }
}
