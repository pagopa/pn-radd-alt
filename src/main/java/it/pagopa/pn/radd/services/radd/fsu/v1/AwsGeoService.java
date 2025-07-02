package it.pagopa.pn.radd.services.radd.fsu.v1;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
public class AwsGeoService {


    public AwsGeoService() {};

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
        throw new NotImplementedException();
    };


}
