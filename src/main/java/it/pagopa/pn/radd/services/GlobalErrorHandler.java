package it.pagopa.pn.radd.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.radd.exception.ExceptionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;


@Slf4j
public class GlobalErrorHandler {

  private final ObjectMapper objectMapper;

  public GlobalErrorHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }


  @NonNull
  public Mono<Void> handle(@NonNull ServerWebExchange serverWebExchange, @NonNull Throwable throwable) {

    DataBufferFactory bufferFactory = serverWebExchange.getResponse().bufferFactory();
    HttpStatus status = ExceptionHelper.getHttpStatusFromException(throwable);
    serverWebExchange.getResponse().setStatusCode(status);
    DataBuffer dataBuffer;
    try {
      dataBuffer = bufferFactory.wrap(objectMapper.writeValueAsBytes(ExceptionHelper.handleException(throwable, status)));
    } catch (JsonProcessingException e) {
      dataBuffer = bufferFactory.wrap("".getBytes());
    }
    serverWebExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return serverWebExchange.getResponse().writeWith(Mono.just(dataBuffer));
  }

}