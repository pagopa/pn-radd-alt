package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.mapper.NormalizedAddressMapper;
import it.pagopa.pn.radd.mapper.RaddRegistryMapper;
import it.pagopa.pn.radd.mapper.RaddRegistryPageMapper;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.pojo.RaddRegistryPage;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {RegistrySelfService.class})
@CustomLog
class RegistrySelfServiceTest {

    @Mock
    private RaddRegistryV2DAO raddRegistryDAO;

    private RegistrySelfService registrySelfService;

    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_UID = "uid";
    public static final Integer LIMIT = 10;
    public static final String LAST_KEY = "lastKey";
    public static final String PARTNER_ID = "partnerId";

    @BeforeEach
    void setUp() {
        registrySelfService = new RegistrySelfService(
                raddRegistryDAO,
                new RaddRegistryPageMapper(new RaddRegistryMapper(new NormalizedAddressMapper()))
        );
    }

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

    private RaddRegistryPage raddRegistryPage() {

        RaddRegistryEntityV2 registry = new RaddRegistryEntityV2();
        registry.setPartnerId(PARTNER_ID);

        List<RaddRegistryEntityV2> listRegistry = new ArrayList<>();
        listRegistry.add(registry);

        RaddRegistryPage page = new RaddRegistryPage();
        page.setItems(listRegistry);
        page.setLastKey(LAST_KEY);

        return page;
    }

    @Test
    void testRetrieveRegistry_success() {

        RaddRegistryPage page = raddRegistryPage();

        when(raddRegistryDAO.findPaginatedByPartnerId(PARTNER_ID, LIMIT, LAST_KEY))
                .thenReturn(Mono.just(page));

        Mono<GetRegistryResponseV2> result = registrySelfService.retrieveRegistry(PARTNER_ID, LIMIT, LAST_KEY);

        StepVerifier.create(result)
                    .assertNext(Assertions::assertNotNull)
                    .verifyComplete();
    }

}