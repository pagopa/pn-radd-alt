    package it.pagopa.pn.radd.services.radd.fsu.v1;

    import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
    import it.pagopa.pn.radd.middleware.db.CoverageDAO;
    import lombok.CustomLog;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import reactor.core.publisher.Mono;

    import java.time.LocalDate;


    @Service
    @RequiredArgsConstructor
    @CustomLog
    public class CoverageService {

        private final CoverageDAO coverageDAO;


        public Mono<CheckCoverageResponse> checkCoverage(CheckCoverageRequest request, SearchMode searchMode) {
            log.info("Checking coverage for cap: {} and locality: {} with search mode: {}",
                     request.getCap(), request.getCity(), searchMode);

            return switch (searchMode) {
                case LIGHT -> handleLightSearch(request);
                case COMPLETE -> handleCompleteSearch(request);
            };
        }

        private Mono<CheckCoverageResponse> handleLightSearch(CheckCoverageRequest request) {
            return coverageDAO.findByCap(request.getCap())
                              .filter(cov -> isValidityActive(cov.getStartValidity(), cov.getEndValidity()))
                              .collectList()
                              .map(coverages -> new CheckCoverageResponse().hasCoverage(!coverages.isEmpty()))
                              .defaultIfEmpty(new CheckCoverageResponse().hasCoverage(false));
        }

        private Mono<CheckCoverageResponse> handleCompleteSearch(CheckCoverageRequest request) {
            return coverageDAO.find(request.getCap(), request.getCity())
                              .map(cov -> new CheckCoverageResponse()
                                      .hasCoverage(isValidityActive(cov.getStartValidity(), cov.getEndValidity())))
                              .defaultIfEmpty(new CheckCoverageResponse().hasCoverage(false));
        }


        private boolean isValidityActive(LocalDate startValidity, LocalDate endValidity) {
            LocalDate today = LocalDate.now();
            //entrambi null -> false
            if (startValidity == null && endValidity == null) {
                return false;
            }
            //solo start valorizzata -> controllo se oggi >= start
            if (startValidity != null && endValidity == null) {
                return !today.isBefore(startValidity);
            }
            // solo end valorizzata -> controllo se oggi <= end
            if (startValidity == null && endValidity != null) {
                return !today.isAfter(endValidity);
            }
            // entrambe valorizate e uguali -> controllo se oggi == quella data
            if (startValidity.equals(endValidity)) {
                return today.equals(startValidity);
            }
            // entrambe valorizzate e diverse -> controllo se oggi è tra start e end (inclusi)
            return !today.isBefore(startValidity) && !today.isAfter(endValidity);
        }

    }