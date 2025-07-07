package it.pagopa.pn.radd.services.radd.fsu.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.util.*;

@Slf4j
@Service
public class AwsGeoService {

    private final GeoPlacesAsyncClient geoPlacesAsyncClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AwsGeoService(GeoPlacesAsyncClient geoPlacesClient) {
        this.geoPlacesAsyncClient = geoPlacesClient;
    }

    public Mono<CoordinatesResult> getCoordinatesForAddress(String address, String province, String zip, String municipality, String country)
             {


        log.info("Input parameters - address: {}, province: {}, zip: {}, municipality: {}, country: {}",
                 address, province, zip, municipality, country);


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
            return Mono.error(new CoordinatesNotFoundException("No geolocation result for address: " + address));
        }
        var geoResult = results.get(0);
        var position = geoResult.position();
        var matchScore = geoResult.matchScores();
        double biasPoint = calculateBiasPoint(matchScore);

        CoordinatesResult result = getCoordinatesResult(position, geoResult, biasPoint, matchScore);

        log.info("AWS geoplacesResult  -> {} ", result.toString());




        return Mono.just(result);
    }

    private CoordinatesResult getCoordinatesResult(List<Double> position, GeocodeResultItem geoResult, double biasPoint, MatchScoreDetails matchScore) {
        // Validazione con raccolta campi mancanti

        String street = extractStreetAndNumber(geoResult.title());
        validateAllFields(geoResult, position, street);

        CoordinatesResult result = new CoordinatesResult();

        result.setAwsLongitude(position.get(0).toString());
        result.setAwsLatitude(position.get(1).toString());
        result.setAwsAddressRow(street);
        result.setAwsPostalCode(geoResult.address().postalCode());
        result.setAwsLocality(geoResult.address().locality());
        result.setAwsSubRegion(geoResult.address().subRegion().code());
        result.setAwsCountry(geoResult.address().country().name());
        result.setBiasPoint((int) Math.round(biasPoint * 100));
        result.setAwsMatchScore(matchScore);

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
        MatchScoreDetails awsMatchScore;
        Integer biasPoint;

    }



    private static void validateAllFields(GeocodeResultItem geoResult, List<Double> position, String street) {
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
            if (street == null || StringUtils.isBlank(street)){
                missingFields.add("street");
            }
        }

        if (!missingFields.isEmpty()) {
            throw new CoordinatesNotFoundException("Missing or empty fields from AWS: " + String.join(", ", missingFields));
        }
    }
}
