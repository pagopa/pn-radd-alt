package it.pagopa.pn.radd.services.radd.fsu.v1.validation;

import it.pagopa.pn.radd.utils.OpeningHoursParser;
import org.apache.commons.lang3.StringUtils;

import static it.pagopa.pn.radd.utils.DateUtils.validateDateInterval;
import static it.pagopa.pn.radd.utils.UrlSanitizer.validateUrl;

public class BaseValidator {

    private BaseValidator() {}

    public static BaseValidator builder() {
        return new BaseValidator();
    }

    public BaseValidator withWebsite(String website) {
        if (StringUtils.isNotBlank(website)) {
            validateUrl(website);
        }
        return this;
    }

    public BaseValidator withOpeningTime(String openingTime) {
        if (StringUtils.isNotBlank(openingTime)) {
            OpeningHoursParser.validateOpenHours(openingTime);
        }
        return this;
    }

    public BaseValidator withDateRange(String startDate, String endDate) {
        validateDateInterval(startDate, endDate);
        return this;
    }

    /**
     * Metodo marker per chiudere la catena di validazione.
     * Le validazioni lanciano eccezioni direttamente nei metodi with*().
     * Questo metodo viene mantenuto per chiarezza semantica del pattern builder.
     * In futuro si può valutare una logica di accumulazione errori.
     */
    public void validate() {
        // Validazioni eseguite nei metodi precedenti
    }
}