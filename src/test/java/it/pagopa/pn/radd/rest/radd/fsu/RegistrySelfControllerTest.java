package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistrySelfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import static org.mockito.Mockito.when;


@ContextConfiguration(classes = {RegistrySelfController.class, RestExceptionHandler.class})
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = {RegistrySelfController.class})
class RegistrySelfControllerTest {

    @MockBean
    private RegistrySelfService registrySelfService;

    @Autowired
    WebTestClient webTestClient;

    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_CX_TYPE = "x-pagopa-pn-cx-type";
    public static final String PN_PAGOPA_UID = "uid";
    public static final String LIMIT = "limit";
    public static final String LASTKEY = "lastKey";
    public static final String CAP = "cap";
    public static final String CITY = "city";
    public static final String PR = "pr";
    public static final String EXTERNALCODE = "externalCode";


    @Test
    void shouldDeleteRegistryAndReturn204() {
        String partnerId = "partnerTest";
        String locationId = "locationTest";

        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        entity.setPartnerId(partnerId);
        entity.setLocationId(locationId);

        when(registrySelfService.deleteRegistry(partnerId, locationId))
                .thenReturn(Mono.just(entity));

        webTestClient.delete()
                     .uri("/radd-net/api/v2/registry/{partnerId}/{locationId}", partnerId, locationId)
                     .header(PN_PAGOPA_CX_TYPE, "PA")
                     .header(PN_PAGOPA_CX_ID, "my-cx-id")
                     .header(PN_PAGOPA_UID, "my-uid")
                     .exchange()
                     .expectStatus().isNoContent();
    }

    @Test
    void shouldReturn204WhenRegistryNotFound() {
        String partnerId = "partnerNotFound";
        String locationId = "locationNotFound";

        when(registrySelfService.deleteRegistry(partnerId, locationId))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                     .uri("/radd-net/api/v2/registry/{partnerId}/{locationId}", partnerId, locationId)
                     .header(PN_PAGOPA_CX_TYPE, "PA")
                     .header(PN_PAGOPA_CX_ID, "my-cx-id")
                     .header(PN_PAGOPA_UID, "my-uid")
                     .exchange()
                     .expectStatus().isNoContent(); // 204
    }

}