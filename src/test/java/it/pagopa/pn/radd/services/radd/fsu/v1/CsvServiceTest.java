package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.pojo.RaddRegistryRequest;
import it.pagopa.pn.radd.pojo.StoreLocatorCsvEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    @InjectMocks
    CsvService csvService;

    @Test
    void readCsvOk() throws IOException {
        File file = new File("src/test/resources", "radd-registry.csv");
        InputStream inputStream = new FileInputStream(file);

        StepVerifier.create(csvService.readItemsFromCsv(RaddRegistryRequest.class, inputStream.readAllBytes(), 1))
                .expectNextMatches(raddRegistryRequests -> raddRegistryRequests.size() == 5)
                .verifyComplete();
    }

    @Test
    void readCsvKo() throws IOException {
        File file = new File("src/test/resources", "radd-registry-error.csv");
        InputStream inputStream = new FileInputStream(file);
        StepVerifier.create(csvService.readItemsFromCsv(RaddRegistryRequest.class, inputStream.readAllBytes(), 1))
                .expectError(RaddGenericException.class);
    }

    @Test
    void writeCsvContentOk() {
        StoreLocatorCsvEntity storeLocatorCsvEntity =
                new StoreLocatorCsvEntity("descrizione", "citta", "via", "provincia", "cap", "telefono", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "latitude", "longitude");
        StoreLocatorCsvEntity storeLocatorCsvEntity2 =
                new StoreLocatorCsvEntity("descrizione2", "citta2", "via2", "provincia2", "cap2", "telefono2", "monday2", "tuesday2", "wednesday2", "thursday2", "friday2", "saturday2", "sunday2", "latitude2", "longitude2");
        List<StoreLocatorCsvEntity> storeLocatorCsvEntityList = new ArrayList<>();
        storeLocatorCsvEntityList.add(storeLocatorCsvEntity);
        storeLocatorCsvEntityList.add(storeLocatorCsvEntity2);

        assertDoesNotThrow(() -> csvService.writeCsvContent(storeLocatorCsvEntityList));
    }

}