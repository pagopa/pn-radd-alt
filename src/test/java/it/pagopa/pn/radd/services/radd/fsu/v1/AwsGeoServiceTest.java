package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.exception.CoordinatesNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.geoplaces.GeoPlacesAsyncClient;
import software.amazon.awssdk.services.geoplaces.model.*;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AwsGeoServiceTest {

    private GeoPlacesAsyncClient mockClient;
    private AwsGeoService awsGeoService;

    @BeforeEach
    void setup() {
        mockClient = Mockito.mock(GeoPlacesAsyncClient.class);
        awsGeoService = new AwsGeoService(mockClient);
    }

    private GeocodeResponse buildGeocodeResponse(String label, String countryCode, String countryName, String subRegionCode, String subRegionName,
                                                 String locality, String postalCode, String street, String addressNumber,
                                                 double lon, double lat, double addressNumberScore,double countryScore,  List<Double> intersectionScores,double postalCodeScore, double subRegionScore , double localityScore, double overall) {

        List<Double> intersectionList = List.of(1.0); // o altri valori realistici

        Address address = Address.builder()
                                 .label(label)
                                 .country(Country.builder().code2(countryCode).name(countryName).build())
                                 .subRegion(SubRegion.builder().code(subRegionCode).name(subRegionName).build())
                                 .locality(locality)
                                 .postalCode(postalCode)
                                 .street(street)
                                 .addressNumber(addressNumber)
                                 .build();

        AddressComponentMatchScores addressMatchScore = AddressComponentMatchScores.builder()
                                                                                   .addressNumber(addressNumberScore)
                                                                                   .postalCode(postalCodeScore)
                                                                                   .subRegion(subRegionScore)
                                                                                   .country(countryScore)
                                                                                   .locality(localityScore)
                                                                                   .intersection(intersectionList)
                                                                                   .build();

        ComponentMatchScores componentMatchScores = ComponentMatchScores.builder()
                                                                        .address(addressMatchScore)
                                                                        .build();

        // ✅ Calcolo media includendo AddressNumber e media di Intersection
        double intersectionAvg = intersectionList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double sum = addressNumberScore + postalCodeScore + subRegionScore + countryScore + intersectionAvg;
        int count = 5;


        MatchScoreDetails matchScores = MatchScoreDetails.builder()
                                                         .components(componentMatchScores)
                                                         .overall(overall)
                                                         .build();

        GeocodeResultItem resultItem = GeocodeResultItem.builder()
                                                        .address(address)
                                                        .position(List.of(lon, lat))
                                                        .matchScores(matchScores)
                                                        .title(label)
                                                        .build();

        return GeocodeResponse.builder()
                              .resultItems(List.of(resultItem))
                              .build();
    }


    @Test
    void testProva1ProvinciaECapErrati() {
        GeocodeResponse response = buildGeocodeResponse(
                "Via Roma, 34, 00010 San Polo dei Cavalieri RM, Italia",
                "IT", "Italia",
                "RM", "Roma",
                "San Polo dei Cavalieri",
                "00010",
                "Via Roma",
                "34",
                12.84315,
                42.00835,
                1,
                1,
                List.of(1.0),
                0.82,
                0.9,
                0,
                0.82
                                                       );

        when(mockClient.geocode(any(GeocodeRequest.class))).thenReturn(Mono.just(response).toFuture());

        Mono<AwsGeoService.CoordinatesResult> resultMono =
                awsGeoService.getCoordinatesForAddress("Via Roma n 34", "FI", "00014", "Roma", "IT");

        StepVerifier.create(resultMono)
                    .assertNext(result -> {
                        Assertions.assertEquals("12.84315", result.getAwsLongitude());
                        Assertions.assertEquals("42.00835", result.getAwsLatitude());
                        Assertions.assertEquals("00010", result.getAwsPostalCode());
                        Assertions.assertEquals("RM", result.getAwsSubRegion());
                        Assertions.assertEquals("San Polo dei Cavalieri", result.getAwsLocality());
                        Assertions.assertTrue(result.getBiasPoint() > 0);

                        var matchScore = result.getAwsMatchScore();
                        Assertions.assertNotNull(matchScore);
                        Assertions.assertEquals(0.82, matchScore.getOverall());
                        Assertions.assertEquals(1.0, matchScore.getComponents().getCountry());
                        Assertions.assertEquals(0.9, matchScore.getComponents().getSubRegion());
                        Assertions.assertEquals(0.82, matchScore.getComponents().getPostalCode());
                        Assertions.assertEquals(1.0, matchScore.getComponents().getAddressNumber());
                    })
                    .verifyComplete();
    }

    @Test
    void testProva2ProvinciaErrata() {
        GeocodeResponse response = buildGeocodeResponse(
                "Via Roma, 34, 00041 Albano Laziale RM, Italia",
                "IT", "Italia",
                "RM", "Roma",
                "Albano Laziale",
                "00041",
                "Via Roma",
                "34",
                12.61756,
                41.72173,
                1.0,
                1,
                List.of(1.0),
                1,
                0.9,0,0.86
                                                       );

        when(mockClient.geocode(any(GeocodeRequest.class))).thenReturn(Mono.just(response).toFuture());

        Mono<AwsGeoService.CoordinatesResult> resultMono =
                awsGeoService.getCoordinatesForAddress("Via Roma 34", "FI", "00041", "Roma", "IT");

        StepVerifier.create(resultMono)
                    .assertNext(result -> {
                        Assertions.assertEquals("12.61756", result.getAwsLongitude());
                        Assertions.assertEquals("41.72173", result.getAwsLatitude());
                        Assertions.assertEquals("00041", result.getAwsPostalCode());
                        Assertions.assertEquals("RM", result.getAwsSubRegion());
                        Assertions.assertEquals("Albano Laziale", result.getAwsLocality());
                        Assertions.assertTrue(result.getBiasPoint() > 0);

                        var matchScore = result.getAwsMatchScore();
                        Assertions.assertNotNull(matchScore);
                        Assertions.assertEquals(0.86, matchScore.getOverall());
                        Assertions.assertEquals(1.0, matchScore.getComponents().getCountry());
                        Assertions.assertEquals(0.9, matchScore.getComponents().getSubRegion());
                        Assertions.assertEquals(1.0, matchScore.getComponents().getPostalCode());
                        Assertions.assertEquals(1.0, matchScore.getComponents().getAddressNumber());
                    })
                    .verifyComplete();
    }

    @Test
    void testProva3IndirizzoAbbreviato() {
        GeocodeResponse response = buildGeocodeResponse(
                "Via Giuseppe Garibaldi, 32, 00047 Marino RM, Italia",
                "IT", "Italia",
                "RM", "Roma",
                "Marino",
                "00047",
                "Via Giuseppe Garibaldi",
                "32",
                12.65886,
                41.77055,
                1.0,
                1.0,
                List.of(1.0),
                1,
                1,
                1,
                1
                                                       );

        when(mockClient.geocode(any(GeocodeRequest.class))).thenReturn(Mono.just(response).toFuture());

        Mono<AwsGeoService.CoordinatesResult> resultMono =
                awsGeoService.getCoordinatesForAddress("Via G. Garibaldi 32", "RM", "00047", "Marino", "IT");

        StepVerifier.create(resultMono)
                    .assertNext(result -> {
                        Assertions.assertEquals("12.65886", result.getAwsLongitude());
                        Assertions.assertEquals("41.77055", result.getAwsLatitude());
                        Assertions.assertEquals("00047", result.getAwsPostalCode());
                        Assertions.assertEquals("RM", result.getAwsSubRegion());
                        Assertions.assertEquals("Marino", result.getAwsLocality());
                        Assertions.assertTrue(result.getBiasPoint() > 0);

                        var matchScore = result.getAwsMatchScore();
                        Assertions.assertNotNull(matchScore);
                        Assertions.assertEquals(1.0, matchScore.getOverall());
                        Assertions.assertEquals(1.0, matchScore.getComponents().getCountry());
                        Assertions.assertEquals(1.0, matchScore.getComponents().getSubRegion());
                        Assertions.assertEquals(1.0, matchScore.getComponents().getPostalCode());
                        Assertions.assertEquals(1.0, matchScore.getComponents().getAddressNumber());
                    })
                    .verifyComplete();
    }

    @Test
    void testProva4IndirizzoInesistente() {
        GeocodeResponse emptyResponse = GeocodeResponse.builder()
                                                       .resultItems(List.of())
                                                       .build();

        when(mockClient.geocode(any(GeocodeRequest.class))).thenReturn(Mono.just(emptyResponse).toFuture());

        Mono<AwsGeoService.CoordinatesResult> resultMono =
                awsGeoService.getCoordinatesForAddress("Via chi lo sa 3040", "MI", "01038", "Firenze", "IT");

        StepVerifier.create(resultMono)
                    .expectError(CoordinatesNotFoundException.class)
                    .verify();

    }

    @Test
    void testCoordinatesNotFoundExceptionWhenMissingFields() {
        // Creo un GeocodeResultItem con indirizzo incompleto (es. postalCode mancante)
        Address address = Address.builder()
                                               .postalCode(null) // campo mancante
                                               .locality("Roma")
                                               .subRegion(SubRegion.builder().code("RM").build())
                                               .country(Country.builder().name("Italia").build())
                                               .build();

        GeocodeResultItem incompleteResultItem = GeocodeResultItem.builder()
                                                                  .address(address)
                                                                  .position(List.of(12.34, 56.78))
                                                                  .title("Via Incompleta, 1")
                                                                  .build();

        GeocodeResponse responseWithMissingFields = GeocodeResponse.builder()
                                                                   .resultItems(List.of(incompleteResultItem))
                                                                   .build();

        // Mocko il client per restituire questa risposta
        when(mockClient.geocode(any(GeocodeRequest.class)))
                .thenReturn(Mono.just(responseWithMissingFields).toFuture());

        // Chiamo il servizio
        Mono<AwsGeoService.CoordinatesResult> resultMono =
                awsGeoService.getCoordinatesForAddress("Via Incompleta, 1", "RM", "00100", "Roma", "IT");

        // Verifico che venga lanciata l’eccezione con messaggio che contiene "postal code"
        StepVerifier.create(resultMono)
                    .expectErrorMatches(throwable ->
                                                throwable instanceof CoordinatesNotFoundException &&
                                                throwable.getMessage().contains("postal code")
                                       )
                    .verify();
    }

    @Test
    void testCoordinatesNotFoundExceptionMultipleMissingFields() {
        // Creo un GeocodeResultItem con postalCode e country mancanti
        Address address = Address.builder()
                                               .locality("Milano")
                                               .subRegion(SubRegion.builder().code("MI").build())
                                                .build();

        GeocodeResultItem incompleteResultItem = GeocodeResultItem.builder()
                                                                  .address(address)
                                                                  .position(List.of(9.19, 45.46))
                                                                  .title("Via Mancante, 10")
                                                                  .build();

        GeocodeResponse responseWithMissingFields = GeocodeResponse.builder()
                                                                   .resultItems(List.of(incompleteResultItem))
                                                                   .build();

        // Mocko il client per restituire questa risposta incompleta
        when(mockClient.geocode(any(GeocodeRequest.class)))
                .thenReturn(Mono.just(responseWithMissingFields).toFuture());

        Mono<AwsGeoService.CoordinatesResult> resultMono =
                awsGeoService.getCoordinatesForAddress("Via Mancante, 10", "MI", "20100", "Milano", "IT");

        StepVerifier.create(resultMono)
                    .expectErrorMatches(throwable ->
                                                throwable instanceof CoordinatesNotFoundException &&
                                                throwable.getMessage().contains("postal code") &&
                                                throwable.getMessage().contains("country")
                                       )
                    .verify();
    }





}
