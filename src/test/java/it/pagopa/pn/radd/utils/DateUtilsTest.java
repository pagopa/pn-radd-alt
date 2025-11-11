package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void testFormatDate_withValidDate() {
        Date now = new Date();
        String result = DateUtils.formatDate(now);
        assertNotNull(result);
        assertTrue(result.contains("T"));
    }

    @Test
    void testFormatDate_withNullDate() {
        assertNull(DateUtils.formatDate(null));
    }

    @Test
    void testParseDateString_withInstantFormat() {
        String input = Instant.now().toString();
        Date result = DateUtils.parseDateString(input);
        assertNotNull(result);
    }

    @Test
    void testParseDateString_withLocalDateTimeFormat() {
        String input = LocalDateTime.now().toString();
        Date result = DateUtils.parseDateString(input);
        assertNotNull(result);
    }

    @Test
    void testParseDateString_withBlankInput() {
        assertNull(DateUtils.parseDateString("  "));
    }

    @Test
    void testGetOffsetDateTime() {
        String input = "2024-10-01T10:15:30";
        OffsetDateTime odt = DateUtils.getOffsetDateTime(input);
        assertEquals(OffsetDateTime.of(2024, 10, 1, 10, 15, 30, 0, ZoneOffset.UTC), odt);
    }

    @Test
    void testGetOffsetDateTimeFromDate() {
        Date now = new Date();
        OffsetDateTime odt = DateUtils.getOffsetDateTimeFromDate(now);
        assertNotNull(odt);
        assertEquals(now.toInstant(), odt.toInstant());
    }

    @Test
    void testGetStartOfDayByInstant() {
        Instant now = Instant.parse("2025-01-01T15:00:00Z");
        Instant startOfDay = DateUtils.getStartOfDayByInstant(now);
        assertEquals("2025-01-01T00:00:00Z", startOfDay.toString());
    }

    @Test
    void testGetStartOfDayToday() {
        Instant start = DateUtils.getStartOfDayToday();
        assertNotNull(start);
        LocalTime time = start.atZone(ZoneOffset.UTC).toLocalTime();
        assertEquals(LocalTime.MIDNIGHT, time);
    }

    @Test
    void testConvertDateToInstantAtStartOfDay_validDate() {
        Instant instant = DateUtils.convertDateToInstantAtStartOfDay("2025-01-01");
        assertEquals("2025-01-01T00:00:00Z", instant.toString());
    }

    @Test
    void testConvertDateToInstantAtStartOfDay_invalidDate() {
        RaddGenericException ex = assertThrows(RaddGenericException.class, () ->
                DateUtils.convertDateToInstantAtStartOfDay("not-a-date"));
        assertEquals(ExceptionTypeEnum.DATE_VALIDATION_ERROR, ex.getExceptionType());
    }

    @Test
    void testValidateEndDate_validInterval() {
        Instant start = LocalDate.parse("2025-01-01").atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = DateUtils.validateEndDate(start, "2025-01-02");
        assertNotNull(end);
    }

    @Test
    void testValidateEndDate_withEndBeforeStart_shouldThrow() {
        Instant start = LocalDate.parse("2025-01-02").atStartOfDay(ZoneOffset.UTC).toInstant();
        RaddGenericException ex = assertThrows(RaddGenericException.class, () ->
                DateUtils.validateEndDate(start, "2025-01-01"));
        assertEquals(ExceptionTypeEnum.DATE_INTERVAL_ERROR, ex.getExceptionType());
    }

    @Test
    void testValidateDateInterval_validRange() {
        String start = LocalDate.now().plusDays(1).toString();
        String end = LocalDate.now().plusDays(2).toString();
        assertDoesNotThrow(() -> DateUtils.validateDateInterval(start, end));
    }

    @Test
    void testValidateDateInterval_invalidStart_shouldThrow() {
        String start = "not-a-date";
        String end = LocalDate.now().plusDays(1).toString();
        RaddGenericException ex = assertThrows(RaddGenericException.class, () ->
                DateUtils.validateDateInterval(start, end));
        assertEquals(ExceptionTypeEnum.DATE_VALIDATION_ERROR, ex.getExceptionType());
    }

    @Test
    void testValidateDateInterval_endBeforeStart_shouldThrow() {
        String start = LocalDate.now().plusDays(2).toString();
        String end = LocalDate.now().plusDays(1).toString();
        RaddGenericException ex = assertThrows(RaddGenericException.class, () ->
                DateUtils.validateDateInterval(start, end));
        assertEquals(ExceptionTypeEnum.DATE_INTERVAL_ERROR, ex.getExceptionType());
    }

    @Test
    void testValidateCoverageDateInterval() {

        //Update senza date
        assertDoesNotThrow(() -> DateUtils.validateCoverageDateInterval(null, null,null,null));

        //Update senza date nell'entità ( 1° aggiornamento con date ) e endDate > startDate
        assertDoesNotThrow(() -> DateUtils.validateCoverageDateInterval(null,null,LocalDate.now(),LocalDate.now().plusDays(1L)));

        //Update senza date nell'entità ( 1° aggiornamento con date ) e endDate = startDate
        assertDoesNotThrow(() -> DateUtils.validateCoverageDateInterval(null,null,LocalDate.now(),LocalDate.now()));

        //Update senza date nell'entità ( 1° aggiornamento con date ) e endDate < startDate
        RaddGenericException ex = assertThrows(RaddGenericException.class, () ->
                DateUtils.validateCoverageDateInterval(null,null,LocalDate.now().plusDays(1L),LocalDate.now()));
        assertEquals(ExceptionTypeEnum.DATE_INTERVAL_ERROR, ex.getExceptionType());

    }


    /**
     * Test per util isValidityActive
     */

    //entrambe le date null -> false
    @Test
    void testBothNull() {
        assertFalse(DateUtils.isDateInRange(null, null, LocalDate.now()));
    }

    //solo start valorizzata -> controllo se oggi >= start
    @Test
    void testOnlyStartPast() {
        LocalDate start = LocalDate.now().minusDays(1);
        assertTrue(DateUtils.isDateInRange(start, null, LocalDate.now()));
    }

    //solo start valorizzata nel futuro -> torno false
    @Test
    void testOnlyStartFuture() {
        LocalDate start = LocalDate.now().plusDays(1);
        assertFalse(DateUtils.isDateInRange(start, null, LocalDate.now()));
    }

    //solo end valorizzata nel passato -> torno false
    @Test
    void testOnlyEndPast() {
        LocalDate end = LocalDate.now().minusDays(1);
        assertFalse(DateUtils.isDateInRange(null, end, LocalDate.now()));
    }

    //solo end valorizzata nel futuro -> torno true
    @Test
    void testOnlyEndFuture() {
        LocalDate end = LocalDate.now().plusDays(1);
        assertTrue(DateUtils.isDateInRange(null, end, LocalDate.now()));
    }

    // entrambe valorizzate e uguali -> controllo se oggi == quella data
    @Test
    void testStartEqualsEndToday() {
        LocalDate date = LocalDate.now();
        assertTrue(DateUtils.isDateInRange(date, date, LocalDate.now()));
    }

    // entrambe valorizzate e uguali nel passato -> torno false
    @Test
    void testStartEqualsEndPast() {
        LocalDate date = LocalDate.now().minusDays(1);
        assertFalse(DateUtils.isDateInRange(date, date, LocalDate.now()));
    }

    // entrambe valorizzate e uguali nel futuro -> torno false
    @Test
    void testStartEqualsEndFuture() {
        LocalDate date = LocalDate.now().plusDays(1);
        assertFalse(DateUtils.isDateInRange(date, date, LocalDate.now()));
    }

    // entrambe valorizzate e diverse -> controllo se oggi è tra start e end (inclusi)
    @Test
    void testRangeIncludesToday() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(1);
        assertTrue(DateUtils.isDateInRange(start, end, LocalDate.now()));
    }

    // entrambe valorizzate e diverse -> oggi non è incluso -> torno false
    @Test
    void testRangeBeforeToday() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now().minusDays(1);
        assertFalse(DateUtils.isDateInRange(start, end, LocalDate.now()));
    }

    // entrambe valorizzate nel futuro -> torno false
    @Test
    void testRangeAfterToday() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(5);
        assertFalse(DateUtils.isDateInRange(start, end, LocalDate.now()));
    }

    //start oggi, end futuro -> torno true
    @Test
    void testRangeStartTodayEndFuture() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(5);
        assertTrue(DateUtils.isDateInRange(start, end, LocalDate.now()));
    }

    //start passato, end oggi -> torno true
    @Test
    void testRangeStartPastEndToday() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        assertTrue(DateUtils.isDateInRange(start, end, LocalDate.now()));
    }

}