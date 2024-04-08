package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.UpdateRegistryRequest;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntity;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistrySelfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {RegistrySelfController.class})
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = {RegistryController.class})
class RegistrySelfControllerTest {

    @MockBean
    private RegistrySelfService registrySelfService;

    @Autowired
    WebTestClient webTestClient;

    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_CX_TYPE = "x-pagopa-pn-cx-type";
    public static final String PN_PAGOPA_UID = "uid";

    @Test
    void updateRegistry() {
        String path = "/radd-alt/api/v1/registry/{registryId}";

        UpdateRegistryRequest request = new UpdateRegistryRequest();
        when(registrySelfService.updateRegistry(any(), any(), any())).thenReturn(Mono.just(mock(RaddRegistryEntity.class)));

        webTestClient.patch()
                .uri(path, "registryId")
                .header(PN_PAGOPA_UID, "myUid")
                .header( PN_PAGOPA_CX_ID, "cxId")
                .header( PN_PAGOPA_CX_TYPE, "PA")
                .body(Mono.just(request), UpdateRegistryRequest.class)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
    }
}