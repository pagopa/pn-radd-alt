package it.pagopa.pn.radd.exception;

import org.springframework.http.HttpStatus;

public class CoverageAlreadyExistsException extends RaddGenericException {

    public CoverageAlreadyExistsException() {
        super(ExceptionTypeEnum.COVERAGE_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }
}
