package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import jakarta.validation.constraints.NotNull;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.DATE_INTERVAL_ERROR;

@Slf4j
public class DateUtils {

    private static final ZoneId italianZoneId =  ZoneId.of("Europe/Rome");

    private DateUtils(){}

    public static String formatDate(Date date)  {
        if (date == null) return null;
        Instant instant = date.toInstant();
        return instant.toString();
    }

    public static Date parseDateString(String date) {
        if (StringUtils.isBlank(date)) return null;
        // se la data finisce per Z, mi aspetto che sia un Istant
        if (date.endsWith("Z"))
            return Date.from(Instant.parse(date));

        // altrimenti è stata salvata nel formato italiano
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime localDate = LocalDateTime.parse(date, formatter);
        ZonedDateTime time = localDate.atZone(italianZoneId);
        return Date.from(time.toInstant());

    }

    public static OffsetDateTime getOffsetDateTime(String date){
        return LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atOffset(ZoneOffset.UTC);
    }

    public static OffsetDateTime getOffsetDateTimeFromDate(Date date) {
        return OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    public static Instant getStartOfDayByInstant(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static Instant getStartOfDayToday() {
        return Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static Instant convertDateToInstantAtStartOfDay(String date) {
        try{
            return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            throw new RaddGenericException(ExceptionTypeEnum.DATE_VALIDATION_ERROR, "La data non è valida (" + date + ")", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Metodo null safe che controlla che le date siano valide e che la data di inizio non sia nel passato.
     * Se la data di inizio è nulla, viene impostata alla data odierna.
     * Se la data di fine è nulla, viene ignorata.
     *
     * @param startDateStr La data di inizio in formato ISO (yyyy-MM-dd).
     * @param endDateStr   La data di fine in formato ISO (yyyy-MM-dd).
     */
    public static void validateDateInterval(String startDateStr, String endDateStr) {
        try {
            log.debug("Validating date interval: start={} end={}", startDateStr, endDateStr);
            Instant start = startDateStr != null ? convertDateToInstantAtStartOfDay(startDateStr) : getStartOfDayToday();
            // Controllo che endDate non sia nel passato rispetto a startDate
            Instant end = null;
            if (StringUtils.isNotBlank(endDateStr))
                end = validateEndDate(start, endDateStr);
            log.debug("Date validation successful: start={} end={}", start, end);
        } catch (DateTimeParseException e) {
            throw new RaddGenericException(ExceptionTypeEnum.DATE_INVALID_ERROR, HttpStatus.BAD_REQUEST);
        }
    }

    public static Instant validateEndDate(@NotNull Instant startDate, @NotNull String endDateStr) {
        log.debug("Validating end date: start={} end={}", startDate, endDateStr);
        Instant end = convertDateToInstantAtStartOfDay(endDateStr);
        if (end.isBefore(startDate)) {
            throw new RaddGenericException(DATE_INTERVAL_ERROR,
                                           "La data di fine validità è precedente a quella di inizio validità (" + startDate + ")",
                                           HttpStatus.BAD_REQUEST);
        }
        return end;
    }

    public static void validateCoverageDateInterval(LocalDate startEntity, LocalDate endEntity, LocalDate startRequest, LocalDate endRequest) {

        log.debug("Starting date validation: startEntity={}, endEntity={}, startRequest={}, endRequest={}",
                startEntity, endEntity, startRequest, endRequest);

        LocalDate effectiveStart = (startRequest != null) ? startRequest : startEntity;
        LocalDate effectiveEnd = (endRequest != null) ? endRequest : endEntity;

        if (!isValidInterval(effectiveStart, effectiveEnd)) {
            String msg = DATE_INTERVAL_ERROR.getMessage() + ": start = " + effectiveStart + ", end = " + effectiveEnd + ".";
            throw new RaddGenericException(DATE_INTERVAL_ERROR, msg, HttpStatus.BAD_REQUEST);
        }

        log.debug("Coverage date validation successful: start={}, end={}", effectiveStart, effectiveEnd);
    }

    public static boolean isValidInterval(LocalDate startValidity, LocalDate endValidity) {
        if (startValidity == null || endValidity == null) {
            return true;
        }
        return startValidity.equals(endValidity) || startValidity.isBefore(endValidity);
    }

    /**
     * Verifica se una data di riferimento è inclusa in un intervallo di date.
     * <p>
     * Se né startDate né endDate sono valorizzati, l'intervallo non è definito
     * e la data di riferimento non è considerata in range (ritorna false).
     *
     * @param startDate Data di inizio dell'intervallo (può essere null, indica nessun limite inferiore)
     * @param endDate Data di fine dell'intervallo (può essere null, indica nessun limite superiore)
     * @param referenceDate Data da verificare se inclusa nell'intervallo (non può essere null)
     * @return true se la data di riferimento è inclusa nell'intervallo, false altrimenti.
     *         Ritorna false se entrambe startDate ed endDate sono null (intervallo non definito).
     */
    public static boolean isDateInRange(LocalDate startDate, LocalDate endDate, @NotNull LocalDate referenceDate) {
        // Se entrambe le date dell'intervallo sono null, l'intervallo non è definito
        // e la data di riferimento non è considerata in range
        if (startDate == null && endDate == null) {
            return false;
        }

        // Se le date di inizio e fine sono uguali, verifica l'uguaglianza esatta
        if (startDate != null && startDate.equals(endDate)) {
            return referenceDate.equals(startDate);
        }

        // Verifica che la data di riferimento sia >= startDate (se presente)
        boolean afterOrEqualStart = startDate == null || !referenceDate.isBefore(startDate);

        // Verifica che la data di riferimento sia <= endDate (se presente)
        boolean beforeOrEqualEnd = endDate == null || !referenceDate.isAfter(endDate);

        return afterOrEqualStart && beforeOrEqualEnd;
    }

}