package it.pagopa.pn.radd.services.radd.fsu.v1;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.pagopa.pn.radd.exception.RaddGenericException;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.opencsv.ICSVParser.DEFAULT_QUOTE_CHARACTER;
import static com.opencsv.ICSVWriter.DEFAULT_ESCAPE_CHARACTER;
import static com.opencsv.ICSVWriter.DEFAULT_LINE_END;


@Component
@RequiredArgsConstructor
@CustomLog
public class CsvService {

    public static final String ERROR_RADD_ALT_READING_CSV = "Error reading CSV: ";
    public static final String ERROR_RADD_ALT_WRITING_CSV = "Error writing CSV: ";
    public static final String PROCESS_START_WRITING_CSV = "[WRITING_CSV] start writing csv";
    public static final String PROCESS_END_WRITING_CSV = "[WRITING_CSV] end writing csv";

    public <T> Mono<List<T>> readItemsFromCsv(Class<T> csvClass, byte[] file, int skipLines) {
        try {
            StringReader stringReader = new StringReader(new String(file, StandardCharsets.UTF_8));
            CsvToBeanBuilder<T> csvToBeanBuilder = new CsvToBeanBuilder<>(stringReader);
            csvToBeanBuilder.withSeparator(';');
            csvToBeanBuilder.withQuoteChar(DEFAULT_QUOTE_CHARACTER);
            csvToBeanBuilder.withSkipLines(skipLines);
            csvToBeanBuilder.withOrderedResults(true);
            csvToBeanBuilder.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS);
            csvToBeanBuilder.withType(csvClass);

            List<T> parsedItems = csvToBeanBuilder.build().parse();
            return Mono.just(new ArrayList<>(parsedItems));
        } catch (Exception e) {
            log.error(ERROR_RADD_ALT_READING_CSV + e.getMessage(), e);
            return Mono.error(new RaddGenericException(ERROR_RADD_ALT_READING_CSV + e.getMessage()));
        }
    }

    public <T> String writeCsvContent(List<T> items) {
        log.logStartingProcess(PROCESS_START_WRITING_CSV);
        try (StringWriter writer = new StringWriter()) {
            CSVWriter cw = new CSVWriter(writer, ';', ICSVWriter.DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(cw)
                    .build();
            beanToCsv.write(items);
            log.logEndingProcess(PROCESS_END_WRITING_CSV);
            log.debug(writer.toString());
            return writer.toString();
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new RaddGenericException(ERROR_RADD_ALT_WRITING_CSV + e.getMessage());
        }
    }
}
