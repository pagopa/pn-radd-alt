package it.pagopa.pn.radd.middleware.msclient;

import it.pagopa.pn.radd.alt.generated.openapi.msclient.pntimelineservice.v1.api.TimelineControllerApi;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pntimelineservice.v1.dto.CancellationRequestResponseDto;
import it.pagopa.pn.radd.exception.PnRaddException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PnTimelineServiceClientTest {

    @Mock
    private TimelineControllerApi timelineControllerApi;

    @InjectMocks
    private PnTimelineServiceClient pnTimelineServiceClient;

    @Test
    void getCancellationRequestReturnsElementWhenResponseIsPresent() {
        CancellationRequestResponseDto responseDto = new CancellationRequestResponseDto();
        when(timelineControllerApi.getCancellationRequest(anyString())).thenReturn(Mono.just(responseDto));

        StepVerifier.create(pnTimelineServiceClient.getCancellationRequest("iun"))
                .expectNext(responseDto)
                .verifyComplete();
    }

    @Test
    void getCancellationRequestReturnsEmptyWhenResponseIs404() {
        WebClientResponseException ex = WebClientResponseException.create(404, "Not Found", null, null, null, null);
        when(timelineControllerApi.getCancellationRequest(anyString())).thenReturn(Mono.error(ex));

        StepVerifier.create(pnTimelineServiceClient.getCancellationRequest("iun"))
                .verifyComplete();
    }

    @Test
    void getCancellationRequestReturnsPnRaddExceptionWhenResponseIsGenericError() {
        WebClientResponseException ex = WebClientResponseException.create(500, "Internal Server Error", null, null, null, null);
        when(timelineControllerApi.getCancellationRequest(anyString())).thenReturn(Mono.error(ex));

        StepVerifier.create(pnTimelineServiceClient.getCancellationRequest("iun"))
                .expectError(PnRaddException.class)
                .verify();
    }
}
