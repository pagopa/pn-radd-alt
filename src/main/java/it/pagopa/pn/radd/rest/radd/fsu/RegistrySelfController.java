package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.api.RegistryApi;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistrySelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegistrySelfController implements RegistryApi {

    private final RegistrySelfService registrySelfService;

}
