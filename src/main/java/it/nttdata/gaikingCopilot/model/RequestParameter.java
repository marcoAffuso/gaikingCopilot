package it.nttdata.gaikingCopilot.model;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

public record RequestParameter(

    String url,
    HttpHeaders headers,
    MultiValueMap<String, String> queryParams,
    String payload,
    MultiValueMap<String, String> payloadMap,  
    int statusCode,
    String response,
    Flux<DataBuffer> dataBufferFlux,   
    String username,
    String password
) {

}
