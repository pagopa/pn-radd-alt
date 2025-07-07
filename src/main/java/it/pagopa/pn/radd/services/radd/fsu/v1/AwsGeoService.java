package it.pagopa.pn.radd.services.radd.fsu.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.radd.exception.CoordinatesNotFoundException;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.geoplaces.GeoPlacesAsyncClient;
import software.amazon.awssdk.services.geoplaces.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AwsGeoService {

    private final GeoPlacesAsyncClient geoPlacesAsyncClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AwsGeoService(GeoPlacesAsyncClient geoPlacesClient) {
        this.geoPlacesAsyncClient = geoPlacesClient;
    }

    public Mono<CoordinatesResult> getCoordinatesForAddress(String address, String province, String zip, String municipality, String country) {

        log.info("##### STARTING AWS GEOLOCATION #####");

        GeocodeRequest request = buildGeocodeRequest(address, province, zip, municipality, country);

        return Mono.fromFuture(geoPlacesAsyncClient.geocode(request))
                   .flatMap(response -> {
                       return getCoordinateResult(response, address);
                   })
                   .doOnError(e -> log.error("Error during AWS geolocation", e));
    }






    private GeocodeQueryComponents buildGeocodeQueryComponents(
            String address, String province, String zip, String municipality, String country) {

        GeocodeQueryComponents.Builder builder = GeocodeQueryComponents.builder();

        if (StringUtils.isNotBlank(country)) builder.country(country);
        if (StringUtils.isNotBlank(province)) builder.subRegion(province);
        if (StringUtils.isNotBlank(zip)) builder.postalCode(zip);
        if (StringUtils.isNotBlank(municipality)) builder.locality(municipality);
        if (StringUtils.isNotBlank(address)) builder.street(address);

        return builder.build();
    }



    private void logGeocodeQueryComponents(GeocodeQueryComponents components) {
        log.info("Geocode query components:");
        log.info("  Street      : {}", components.street());
        log.info("  Postal Code : {}", components.postalCode());
        log.info("  SubRegion   : {}", components.subRegion());
        log.info("  Locality    : {}", components.locality());
        log.info("  Country     : {}", components.country());
    }



    public double calculateBiasPoint(MatchScoreDetails matchScore) {
        if (matchScore == null || matchScore.components() == null || matchScore.components().address() == null) {
            return 0.0;
        }

        var addressComponents = matchScore.components().address();

        double addressNumberScore = Optional.ofNullable(addressComponents.addressNumber()).orElse(0.0);
        double postalCodeScore = Optional.ofNullable(addressComponents.postalCode()).orElse(0.0);
        double subRegionScore = Optional.ofNullable(addressComponents.subRegion()).orElse(0.0);
        double countryScore = Optional.ofNullable(addressComponents.country()).orElse(0.0);

        return (0.10 * addressNumberScore) +
               (0.20 * countryScore) +
               (0.35 * postalCodeScore) +
               (0.35 * subRegionScore);
    }




    private String extractStreetAndNumber(String awsAddressRow) {
        if (StringUtils.isBlank(awsAddressRow)) {
            return "";
        }

        // Esempio: "Via Roma, 34, 00041 Albano Laziale RM, Italia"
        String[] parts = awsAddressRow.split(",");
        if (parts.length >= 2) {
            return parts[0].trim() + ", " + parts[1].trim();  // "Via Roma, 34"
        } else {
            return awsAddressRow.trim();
        }
    }



    private GeocodeRequest buildGeocodeRequest(
            String address, String province, String zip, String municipality, String country) {

        GeocodeQueryComponents components = buildGeocodeQueryComponents(address, province, zip, municipality, country);

        logGeocodeQueryComponents(components);

        GeocodeFilter filter = GeocodeFilter.builder()
                                            .includeCountries("IT")
                                            .build();


        return GeocodeRequest.builder()
                             .maxResults(1)
                             .filter(filter)
                             .queryComponents(components)
                             .language("it")
                             .build();
    }




    private Mono<CoordinatesResult> getCoordinateResult ( GeocodeResponse response, String address ){

        var results = response.resultItems();
        if (results == null || results.isEmpty()) {
            log.warn("No geolocation result for address: {}", address);
            log.info("##### END AWS GEOLOCATION #####");
            return Mono.error(new CoordinatesNotFoundException("No geolocation result for address: " + address));
        }
        var geoResult = results.get(0);
        var position = geoResult.position();
        var matchScore = geoResult.matchScores();
        double biasPoint = calculateBiasPoint(matchScore);

        CoordinatesResult result = getCoordinatesResult(position, geoResult, biasPoint, matchScore);

        try {
            String jsonResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            log.info("AWS GEOPLACES RESULT -> {}", jsonResult);
            log.info("##### END AWS GEOLOCATION #####");

        } catch (Exception e) {
            log.error("##### ERROR AWS GEOLOCATION #####");
            log.error("Error serializing CoordinatesResult to JSON", e);
            log.error("Fallback log: {}", result);



        }

        return Mono.just(result);
    }

    private static CoordinatesResult getCoordinatesResult(List<Double> position, GeocodeResultItem geoResult, double biasPoint, MatchScoreDetails matchScore) {
        // Validazione con raccolta campi mancanti
        validateAllFields(geoResult, position);

        CoordinatesResult result = new CoordinatesResult();

        result.setAwsLongitude(position.get(0).toString());
        result.setAwsLatitude(position.get(1).toString());
        result.setAwsAddressRow(Optional.ofNullable(geoResult.title()).orElse(""));
        result.setAwsPostalCode(geoResult.address().postalCode());
        result.setAwsLocality(geoResult.address().locality());
        result.setAwsSubRegion(geoResult.address().subRegion().code());
        result.setAwsCountry(geoResult.address().country().name());
        result.setBiasPoint((int) Math.round(biasPoint * 100));
        result.setAwsMatchScore(getMatchScore(matchScore));

        return result;
    }


    @Data
    @ToString
    public static class CoordinatesResult {
        String awsAddressRow;
        String awsPostalCode;
        String awsLocality;
        String awsSubRegion;
        String awsCountry;
        String awsLongitude;
        String awsLatitude;
        private SerializableMatchScores awsMatchScore;
        Integer biasPoint;

    }



    private static SerializableMatchScores getMatchScore(MatchScoreDetails matchScore) {
        var awsScore = new SerializableMatchScores();

        if (matchScore != null && matchScore.components() != null && matchScore.components().address() != null) {
            awsScore.setOverall(matchScore.overall());

            var addressRaw = matchScore.components().address();
            var addressComponents = new SerializableMatchScores.AddressComponents();

            addressComponents.setAddressNumber(addressRaw.addressNumber());
            addressComponents.setCountry(addressRaw.country());
            addressComponents.setPostalCode(addressRaw.postalCode());
            addressComponents.setSubRegion(addressRaw.subRegion());
            addressComponents.setIntersection(addressRaw.intersection());

            awsScore.setComponents(addressComponents);


        }
        return awsScore;
    }



    @Data
    public static class SerializableMatchScores {
        private AddressComponents components;
        private Double overall;

        @Data
        public static class AddressComponents {
            private Double addressNumber;
            private Double country;
            private List<Double> intersection;
            private Double postalCode;
            private Double subRegion;
        }
    }


    private static void validateAllFields(GeocodeResultItem geoResult, List<Double> position) {
        List<String> missingFields = new ArrayList<>();

        if (position == null || position.size() < 2 || position.get(0) == null || position.get(1) == null) {
            missingFields.add("coordinates (longitude and latitude)");
        }

        if (geoResult.address() == null) {
            missingFields.add("address");
        } else {
            if (StringUtils.isBlank(geoResult.address().postalCode())) {
                missingFields.add("postal code");
            }
            if (StringUtils.isBlank(geoResult.address().locality())) {
                missingFields.add("locality");
            }
            if (geoResult.address().subRegion() == null || StringUtils.isBlank(geoResult.address().subRegion().code())) {
                missingFields.add("subRegion");
            }
            if (geoResult.address().country() == null || StringUtils.isBlank(geoResult.address().country().name())) {
                missingFields.add("country");
            }
        }

        if (!missingFields.isEmpty()) {
            throw new CoordinatesNotFoundException("Missing or empty fields: " + String.join(", ", missingFields));
        }
    }
}
