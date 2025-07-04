package it.pagopa.pn.radd.services.radd.fsu.v1;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.geoplaces.GeoPlacesAsyncClient;
import software.amazon.awssdk.services.geoplaces.model.*;

import java.util.Optional;


@Slf4j
@Service
public class AwsGeoService {
    public GeoPlacesAsyncClient geoPlacesAsyncClient;

    public AwsGeoService(GeoPlacesAsyncClient geoPlacesClient) {
        this.geoPlacesAsyncClient= geoPlacesClient;
    };

    public AwsGeoService() {};

    @ToString
    public static class CoordinatesResult {
        String awsLongitude;
        String awsLatitude;
        String awsAddressRow;
        String awsPostalCode;
        String awsLocality;
        String awsSubRegion;
        String awsCountry;
        Integer biasPoint;


        public CoordinatesResult(String awsLongitude,String awsLatitude,String awsAddressRow,String awsPostalCode,String awsLocality,String awsSubRegion,String awsCountry,Integer biasPoint) {
            this.awsLongitude = awsLongitude;
            this.awsLatitude = awsLatitude;
            this.awsAddressRow = awsAddressRow;
            this.awsPostalCode = awsPostalCode;
            this.awsLocality = awsLocality;
            this.awsSubRegion = awsSubRegion;
            this.awsCountry = awsCountry;
            this.biasPoint = biasPoint;
        }
    }

    //Metodo effettivo per recuperare gelocalizzazione e normalizzazione indirizzo
    public Mono<CoordinatesResult> getCoordinatesForAddress(String address, String province, String zip, String municipality) {


//        if (geoPlacesAsyncClient == null) {
//            return Mono.error(new IllegalStateException("GeoPlacesAsyncClient non inizializzato"));
//        }

        // Costruisco QueryComponents con campi strutturati
        GeocodeQueryComponents.Builder queryComponentsBuilder = GeocodeQueryComponents.builder();

        if (StringUtils.isNotBlank(address)) queryComponentsBuilder.street(address);
        if (StringUtils.isNotBlank(province)) queryComponentsBuilder.subRegion(province);
        if (StringUtils.isNotBlank(zip)) queryComponentsBuilder.postalCode(zip);
        if (StringUtils.isNotBlank(municipality)) queryComponentsBuilder.locality(municipality);

        // Costruisco filtro su paese Italia
        GeocodeFilter filter = GeocodeFilter.builder()
                                            .includeCountries("IT")
                                            .build();

        // Creo richiesta geocoding con parametri
        GeocodeRequest request = GeocodeRequest.builder()
                                               .maxResults(1)
                                               .filter(filter)
                                               .queryComponents(queryComponentsBuilder.build())
                                               .language("it")
                                               .build();

        return Mono.fromFuture(geoPlacesAsyncClient.geocode(request))
                   .flatMap(response -> {
                       var results = response.resultItems();
                       if (results == null || results.isEmpty()) {
                           log.warn("Nessun risultato per indirizzo: {}", address);
                           return Mono.empty();
                       }

                       var geoResponse = results.get(0);
//                       var place = first.place();

                       var position = geoResponse.position();

                       MatchScoreDetails matchScore = geoResponse.matchScores();

                       Double biasPoint = calculateBiasPoint(matchScore);




                       // Campi di indirizzo AWS
                       String awsPostalCode = Optional.ofNullable(geoResponse.address().postalCode()).orElse("");
                       String awsSubRegion = Optional.ofNullable(geoResponse.address().subRegion().code()).orElse("");
                       String awsLocality = Optional.ofNullable(geoResponse.address().locality()).orElse("");
                       String awsCountry = Optional.ofNullable(geoResponse.address().country().name()).orElse("");
                       String awsAddressRow = geoResponse.address().street() != null && geoResponse.address().addressNumber() !=null  ? geoResponse.address().street()+", "+geoResponse.address().addressNumber() : "";

                       // Controllo corrispondenza provincia e CAP - abbasso score se mismatch



                       CoordinatesResult result = new CoordinatesResult(
                               position != null && !position.isEmpty() ? position.get(0).toString() : null,
                               position != null && position.size() > 1 ? position.get(1).toString() : null,
                               awsAddressRow,
                               awsPostalCode,
                               awsLocality,
                               awsSubRegion,
                               awsCountry,
                               (int) Math.round(biasPoint)
                       );

                       log.info("RESULT -> {}", result);

                       return Mono.just(result);
                   })
                   .doOnError(e -> log.error("Errore nella geolocalizzazione AWS", e));
    }


    public double calculateBiasPoint(MatchScoreDetails matchScore) {
        if (matchScore == null || matchScore.components() == null || matchScore.components().address() == null) {
            return 0.0;
        }

        var addressComponents = matchScore.components().address();

        // Prendo i singoli punteggi, metto 0 se null
        double addressNumberScore = addressComponents.addressNumber() != null ? addressComponents.addressNumber() : 0;
        double postalCodeScore = addressComponents.postalCode() != null ? addressComponents.postalCode() : 0;
        double localityScore = addressComponents.locality() != null ? addressComponents.locality() : 0;

        // Intersection è una lista, sommo i valori se presenti
        int intersectionScore = 0;
        if (addressComponents.intersection() != null) {
            intersectionScore = addressComponents.intersection().stream()
                                                 .mapToInt(Double::intValue)
                                                 .sum();
        }

        // Definisco un punteggio massimo possibile, ad esempio
        int maxAddressNumber = 1;  // come da esempio JSON
        int maxPostalCode = 1;
        int maxLocality = 1;
        int maxIntersection = 1 * (addressComponents.intersection() != null ? addressComponents.intersection().size() : 0);

        int maxTotal = maxAddressNumber + maxPostalCode + maxLocality + maxIntersection;
        Double actualTotal = addressNumberScore + postalCodeScore + localityScore + intersectionScore;

        // Calcolo la percentuale di match (biasPoint)
        double biasPointPercentage = 0.0;
        if (maxTotal > 0) {
            biasPointPercentage = (actualTotal * 100.0) / maxTotal;
        }

        return biasPointPercentage;
    }

}
