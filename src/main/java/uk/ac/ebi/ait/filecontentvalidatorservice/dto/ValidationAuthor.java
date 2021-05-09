package uk.ac.ebi.ait.filecontentvalidatorservice.dto;

public enum ValidationAuthor {
    Core,
    Taxonomy,
    Ena,
    Eva,
    Biosamples,
    BioStudies,
    ArrayExpress,
    Metabolights,
    Pride,
    FileReference,
    FileContent,
    JsonSchema;

    private ValidationAuthor() {
    }
}