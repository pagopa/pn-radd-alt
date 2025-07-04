package it.pagopa.pn.radd.services.radd.fsu.v1;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.geoplaces.GeoPlacesAsyncClient;
import software.amazon.awssdk.services.geoplaces.model.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
class AwsGeoServiceTest {

    @Mock
    private GeoPlacesAsyncClient geoPlacesAsyncClient;

    @InjectMocks
    private AwsGeoService awsGeoService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnCoordinatesWhenValidResponse() {
        // Arrange
        Address address = Address.builder()
                                 .label("Viale Roma, 2, 00199 Roma RM, Italia")
                                 .country(Country.builder()
                                                 .code2("IT")
                                                 .code3("ITA")
                                                 .name("Italia")
                                                 .build())
                                 .region(Region.builder()
                                               .name("Lazio")
                                               .build())
                                 .subRegion(SubRegion.builder()
                                                     .code("RM")
                                                     .name("Roma")
                                                     .build())
                                 .locality("Roma")
                                 .district("Trieste")
                                 .postalCode("00199")
                                 .street("Viale Roma")
                                 .addressNumber("2")
                                 .build();

        AddressComponentMatchScores addressComponents = AddressComponentMatchScores.builder()
                                                                                   .addressNumber(1.0)
                                                                                   .intersection(List.of(1.0))
                                                                                   .locality(1.0)
                                                                                   .postalCode(1.0)
                                                                                   .build();

        ComponentMatchScores components = ComponentMatchScores.builder()
                                                              .address(addressComponents)
                                                              .build();


        MatchScoreDetails matchScoreDetails = MatchScoreDetails.builder()
                                                               .components(components)
                                                               .overall(1.0)
                                                               .build();

        GeocodeResultItem resultItem = GeocodeResultItem.builder()
                                                        .address(address)
                                                        .distance(956692L)
                                                        .mapView(List.of(12.52446, 41.92941, 12.52688, 41.93121))
                                                        .matchScores(matchScoreDetails)
                                                        .placeId("AQAAAF4A25IuvLKN19Ag9inAmWCt6il2dvvc4JZm7Z5cFrWkHL5l68OOBU656kaAN9kQyeU3IVfH19dPSbXeTFHT8U_-37QNXRCki8nhPSW82GT8oLekAF_VhMEGw-_Yq66bKux7va7hsdmSabMwiTWFq9lmrm66dAb3-J77gGCeDVpI")
                                                        .placeType("PointAddress")
                                                        .position(List.of(12.52567, 41.93031))
                                                        .title("Viale Roma, 2, 00199 Roma RM, Italia")
                                                        .build();

        GeocodeResponse response = GeocodeResponse.builder()
                                                  .resultItems(List.of(resultItem))
                                                  .build();

        when(geoPlacesAsyncClient.geocode(any(GeocodeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act
        Mono<AwsGeoService.CoordinatesResult> resultMono = awsGeoService.getCoordinatesForAddress("Viale Roma 2", "RM", "00199", "Roma");

        // Assert
        StepVerifier.create(resultMono)
                    .expectNextMatches(res ->
                                               res.biasPoint == 100  // biasPoint ora è in percentuale calcolata da componenti
                                      )
                    .verifyComplete();
    }


    @Test
    void shouldReturnEmptyWhenNoResults() {
        // Arrange
        GeocodeResponse response = GeocodeResponse.builder()
                                                  .resultItems(List.of())
                                                  .build();

        when(geoPlacesAsyncClient.geocode(any(GeocodeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act
        Mono<AwsGeoService.CoordinatesResult> resultMono =
                awsGeoService.getCoordinatesForAddress("Indirizzo inesistente", "RM", "00100", "Roma");

        // Assert
        StepVerifier.create(resultMono)
                    .verifyComplete();
    }



    @Test
    void shouldLowerScoreOnCapMismatch() {
        // Arrange: costruisco l'indirizzo con postalCode "99999" (diverso dal "00100" in input)
        Address address = Address.builder()
                                 .postalCode("99999")  // CAP che provoca mismatch
                                 .subRegion(SubRegion.builder().code("RM").build())
                                 .locality("Roma")
                                 .street("Via Roma")
                                 .addressNumber("1")
                                 .country(Country.builder().name("Italia").build())
                                 .build();

        // Coordinate fittizie
        List<Double> fakePosition = List.of(12.5, 41.9);

        // Creo un resultItem con i dati dell'indirizzo e posizione
        GeocodeResultItem resultItem = GeocodeResultItem.builder()
                                                        .address(address)
                                                        .position(fakePosition)
                                                        .title("Via Roma 1, Roma")
                                                        .build();

        GeocodeResponse response = GeocodeResponse.builder()
                                                  .resultItems(List.of(resultItem))
                                                  .build();

        // Mock della chiamata AWS Location
        when(geoPlacesAsyncClient.geocode(any(GeocodeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act: chiamo il metodo con il CAP atteso "00100"
        Mono<AwsGeoService.CoordinatesResult> resultMono =
                awsGeoService.getCoordinatesForAddress("Via Roma", "RM", "00100", "Roma");

        StepVerifier.create(resultMono)
                    .assertNext(res -> {
                        Assertions.assertEquals("12.5", res.awsLongitude);
                        Assertions.assertEquals("41.9", res.awsLatitude);
                        Assertions.assertEquals("Via Roma, 1", res.awsAddressRow);
                        Assertions.assertEquals("99999", res.awsPostalCode); // valore di AWS
                        Assertions.assertEquals("Roma", res.awsLocality);
                        Assertions.assertEquals("RM", res.awsSubRegion);
                        Assertions.assertEquals("Italia", res.awsCountry);
                        Assertions.assertEquals(0, res.biasPoint);
                    })
                    .verifyComplete();
    }

    @Test
    void shouldReturnBiasPointLowerThan80WithPartialMatch() {
        // Arrange: costruisco l'indirizzo di risposta AWS
        Address address = Address.builder()
                                 .postalCode("00100")
                                 .subRegion(SubRegion.builder().code("RM").build())
                                 .locality("Roma")
                                 .street("Via Roma")
                                 .addressNumber("1")
                                 .country(Country.builder().name("Italia").build())
                                 .build();

        List<Double> fakePosition = List.of(12.5, 41.9);

        // Simulo un match parziale (punteggi bassi)
        AddressComponentMatchScores addressComponentMatchScores = AddressComponentMatchScores.builder()
                                                                                             .addressNumber(0.2)
                                                                                             .postalCode(0.4)
                                                                                             .locality(0.5)
                                                                                             .build();

        ComponentMatchScores componentMatchScores = ComponentMatchScores.builder()
                                                                        .address(addressComponentMatchScores)
                                                                        .build();

        MatchScoreDetails matchScores = MatchScoreDetails.builder()
                                                         .components(componentMatchScores)
                                                         .build();

        // Creo GeocodeResultItem con matchScores incluso
        GeocodeResultItem resultItem = GeocodeResultItem.builder()
                                                        .address(address)
                                                        .position(fakePosition)
                                                        .title("Via Roma 1, Roma")
                                                        .matchScores(matchScores)
                                                        .build();

        GeocodeResponse response = GeocodeResponse.builder()
                                                  .resultItems(List.of(resultItem))
                                                  .build();

        when(geoPlacesAsyncClient.geocode(any(GeocodeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act
        Mono<AwsGeoService.CoordinatesResult> resultMono =
                awsGeoService.getCoordinatesForAddress("Via Roma", "RM", "00100", "Roma");

        // Assert
        StepVerifier.create(resultMono)
                    .assertNext(res -> {
                        Assertions.assertEquals("12.5", res.awsLongitude);
                        Assertions.assertEquals("41.9", res.awsLatitude);
                        Assertions.assertEquals("Via Roma, 1", res.awsAddressRow);
                        Assertions.assertEquals("00100", res.awsPostalCode);
                        Assertions.assertEquals("Roma", res.awsLocality);
                        Assertions.assertEquals("RM", res.awsSubRegion);
                        Assertions.assertEquals("Italia", res.awsCountry);
                        Assertions.assertTrue(res.biasPoint > 0 && res.biasPoint < 80,
                                              "Il biasPoint dovrebbe essere > 0 e < 80, ma è: " + res.biasPoint);
                    })
                    .verifyComplete();
    }



}
