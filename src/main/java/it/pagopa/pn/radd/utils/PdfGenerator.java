package it.pagopa.pn.radd.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@Slf4j
public class PdfGenerator {
    private static final String FIELD_DENOMINATION = "denomination";
    private static final String FIELD_NUMBER_OF_PAGES = "numberOfPages";

    private final DocumentComposition documentComposition;

    public PdfGenerator(DocumentComposition documentComposition) {
        this.documentComposition = documentComposition;
    }

    public byte[] generateCoverFile(String denomination, Integer numberOfPages) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_DENOMINATION, denomination);
        if (numberOfPages != null) { templateModel.put(FIELD_NUMBER_OF_PAGES, numberOfPages); }
        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.COVER_FILE,
                templateModel
        );

    }
}

