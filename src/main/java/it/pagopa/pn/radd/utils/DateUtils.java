package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

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
        //return OffsetDateTime.ofInstant(date.toInstant(), italianZoneId);
        return OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    public static Instant getStartOfDayByInstant(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static Instant getStartOfDayToday() {
        return Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static Instant convertDateToInstantAtStartOfDay(String date) {
        return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Controlla che le date siano valide e che la data di inizio non sia nel passato.
     * Se la data di inizio è nulla, viene impostata alla data odierna.
     *
     * @param startDateStr La data di inizio in formato ISO (yyyy-MM-dd).
     * @param endDateStr   La data di fine in formato ISO (yyyy-MM-dd), opzionale.
     */
    public static void validateDateInterval(String startDateStr, String endDateStr) {
        try {
            Instant today = getStartOfDayToday();
            Instant start = startDateStr != null ? convertDateToInstantAtStartOfDay(startDateStr) : today;

            if (start.isBefore(today)) {
                throw new RaddGenericException(ExceptionTypeEnum.START_VALIDITY_IN_THE_PAST, HttpStatus.BAD_REQUEST);
            }

            Instant end = null;
            if (endDateStr != null) {
                end = convertDateToInstantAtStartOfDay(endDateStr);
                if (end.isBefore(start)) {
                    throw new RaddGenericException(ExceptionTypeEnum.DATE_INTERVAL_ERROR, HttpStatus.BAD_REQUEST);
                }
            }
            log.debug("Date validation successful: start={} end={}", start, end);
        } catch (DateTimeParseException e) {
            throw new RaddGenericException(ExceptionTypeEnum.DATE_INVALID_ERROR, HttpStatus.BAD_REQUEST);
        }
    }

    public static void validateDateInterval(Instant startDate, String endDateStr) {
        LocalDate localDate = startDate.atZone(ZoneId.systemDefault()).toLocalDate();
        String startDateStr = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        validateDateInterval(startDateStr, endDateStr);
    }
}
