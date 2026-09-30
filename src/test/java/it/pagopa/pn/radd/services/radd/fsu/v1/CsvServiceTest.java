package it.pagopa.pn.radd.services.radd.fsu.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.pojo.RaddRegistryRequest;
import it.pagopa.pn.radd.pojo.StoreLocatorCsvConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    @InjectMocks
    CsvService csvService;

    @Test
    void readCsvOk() throws IOException {
        File file = new File("src/test/resources", "radd-registry.csv");
        InputStream inputStream = new FileInputStream(file);

        StepVerifier.create(csvService.readItemsFromCsv(RaddRegistryRequest.class, inputStream.readAllBytes(), 1))
                .expectNextMatches(raddRegistryRequests -> raddRegistryRequests.size() == 8)
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
    void writeItemsOnCsvToString() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<String[]> list = new ArrayList<>();

        ClassPathResource inputConfigResource = new ClassPathResource("csvConfig.json");
        byte[] config = Files.readAllBytes(inputConfigResource.getFile().toPath());
        String json = new String(config, StandardCharsets.UTF_8);
        List<StoreLocatorCsvConfig> storeLocatorCsvConfigList = objectMapper.readValue(json, new TypeReference<>() {});
        storeLocatorCsvConfigList.forEach(storeLocatorCsvConfig -> storeLocatorCsvConfig.setValue("test"));
        String[] headers = storeLocatorCsvConfigList.stream().map(StoreLocatorCsvConfig::getHeader).toList().toArray(new String[0]);
        String[] row = storeLocatorCsvConfigList.stream().map(StoreLocatorCsvConfig::getValue).toList().toArray(new String[0]);
        list.add(headers);
        list.add(row);

        ClassPathResource inputResource = new ClassPathResource("writeCsvWithArray.csv");
        byte[] csvContent = Files.readAllBytes(inputResource.getFile().toPath());

        String content = csvService.writeCsvContentFromArray(list);
        Assertions.assertEquals(new String(csvContent), content);
    }

}