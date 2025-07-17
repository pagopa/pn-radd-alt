package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistrySelfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ContextConfiguration(classes = {RegistrySelfController.class, RestExceptionHandler.class})
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = {RegistrySelfController.class})
class RegistrySelfControllerTest {

    @MockBean
    private RegistrySelfService registrySelfService;

    @Autowired
    WebTestClient webTestClient;

    private final String PARTNER_ID = "partnerId";
    public static final String LAST_KEY = "lastKey";

    private final String GET_PATH = "/radd-net/api/v2/registry/{partnerId}";


    private GetRegistryResponseV2 getRegistryResponseV2() {

        RegistryV2 registry = new RegistryV2();
        registry.setPartnerId(PARTNER_ID);

        List<RegistryV2> listRegistry = new ArrayList<>();
        listRegistry.add(registry);

        GetRegistryResponseV2 res = new GetRegistryResponseV2();
        res.setItems(listRegistry);
        res.setLastKey(LAST_KEY);

        return res;
    }

    @Test
    void retrieveRegistry_success() {

        Mockito.when(registrySelfService.retrieveRegistry(eq(PARTNER_ID), any(), any()))
               .thenReturn(Mono.just(getRegistryResponseV2()));

        webTestClient.get()
                     .uri(GET_PATH, PARTNER_ID)
                     .header("x-pagopa-pn-cx-type", "RADD")
                     .header("x-pagopa-pn-cx-id", "test-cx-id")
                     .header("uid", "test-uid")
                     .exchange()
                     .expectStatus()
                     .isOk();

    }

}