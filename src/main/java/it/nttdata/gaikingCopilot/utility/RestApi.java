package it.nttdata.gaikingCopilot.utility;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

@Component
@Log4j2
public class RestApi {

    @Setter
    private String url;

    @Setter
    private HttpHeaders headers;

    @Setter
    private MultiValueMap<String, String> queryParams;

    @Setter
    private String payload;

    @Setter
    private MultiValueMap<String, String> payloadMap;

    @Getter
    private int statusCode;

    @Getter
    private String response;

    @Getter 
    private Flux<DataBuffer> dataBufferFlux;    

    @Setter
    private String username;

    @Setter
    private String password;

    private int timeout = 200000;


    private String getBasicAuthHeader() {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedAuth);
    }

    public WebClient configureWebClient() {

        int bufferSize = 100 * 1024 * 1024; // 100 MB
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSize))
                .build();

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .secure(sslContextSpec -> {
                        try {
                                sslContextSpec.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
                            } catch (SSLException e) {
                                log.error(e.getMessage());
                            }
                    })
                    //.followRedirect(true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                    .responseTimeout(Duration.ofMillis(timeout))
                    .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS)))
                ))
            .exchangeStrategies(exchangeStrategies)
            .build();

    }

    public void requestPost(){
        WebClient webClient = this.configureWebClient();
        ResponseEntity<String> responseEntity = webClient.post()
                    .uri(this.url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(this.payload)
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class))
                    .block();

        if (responseEntity != null) {
            this.statusCode = responseEntity.getStatusCode().value();
            this.response = responseEntity.getBody();
        }
    }

    public void requestGet(){

        WebClient webClient = this.configureWebClient();
        ResponseEntity<String> responseEntity = webClient.get()
                .uri(this.url)
                .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class))
                .block();

            if (responseEntity != null) {
                this.statusCode = responseEntity.getStatusCode().value();
                this.response = responseEntity.getBody();
            }

    }

    public void requestPostWithHeadersWithoutParameters() {
        WebClient webClient = this.configureWebClient();

        ResponseEntity<String> responseEntity = webClient.post()
                    .uri(this.url)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .bodyValue(this.payload)
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class))
                    .block();

        if (responseEntity != null) {
            this.statusCode = responseEntity.getStatusCode().value();
            this.response = responseEntity.getBody();
        }
    }

    public void requestPostWithHeadersAndParameters(){
        WebClient webClient = this.configureWebClient();
        ResponseEntity<String> responseEntity = webClient.post()
                    .uri(UriComponentsBuilder.fromUriString(this.url).queryParams(this.queryParams).build().toUri())
                    .headers(httpHeaders ->{ 
                        httpHeaders.addAll(headers); 
                        if(this.username != null && this.password != null) {
                            httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                        }
                    })
                    .bodyValue(this.payload)
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class))
                    .block();

        if (responseEntity != null) {
            this.statusCode = responseEntity.getStatusCode().value();
            this.response = responseEntity.getBody();
        }
    }

    public void requestPostUrlEncoded(){
        WebClient webClient = this.configureWebClient();
            ResponseEntity<String> responseEntity = webClient.post()
                    .uri(UriComponentsBuilder.fromUriString(this.url).queryParams(this.queryParams).build().toUri())
                    .headers(httpHeaders ->{ 
                        httpHeaders.addAll(headers); 
                        if(this.username != null && this.password != null) {
                            httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                        }
                    })
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(this.payloadMap))
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class))
                    .block();

            if (responseEntity != null) {
                this.statusCode = responseEntity.getStatusCode().value();
                this.response = responseEntity.getBody();
            }
    }

    public void requestGetWithHeadersAndParameters(){
        WebClient webClient = this.configureWebClient();
        ResponseEntity<String> responseEntity = webClient.get()
                .uri(UriComponentsBuilder.fromUriString(this.url).queryParams(this.queryParams).build().toUri())
                .headers(httpHeaders ->{ 
                        httpHeaders.addAll(headers); 
                        if(this.username != null && this.password != null) {
                            httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                        }
                    })
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class))
                    .block();

        if (responseEntity != null) {
            this.statusCode = responseEntity.getStatusCode().value();
            this.response = responseEntity.getBody();
        }

    }

    public void requestPatchWithHeadersAndParameters(){
        WebClient webClient = this.configureWebClient();
            ResponseEntity<String> responseEntity = webClient.patch()
                    .uri(UriComponentsBuilder.fromUriString(this.url).queryParams(this.queryParams).build().toUri())
                    .headers(httpHeaders ->{ 
                        httpHeaders.addAll(headers); 
                        if(this.username != null && this.password != null) {
                            httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                        }
                    })
                    .bodyValue(this.payload)
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class))
                    .block();

        if (responseEntity != null) {
            this.statusCode = responseEntity.getStatusCode().value();
            this.response = responseEntity.getBody();
        }
    }


    public void downloadApp() {

        WebClient webClient = this.configureWebClient();
        URI uri = UriComponentsBuilder.fromUriString(this.url).queryParams(queryParams).build().toUri();
        log.info("URL: " + this.url);
        log.info("Query Params: " + queryParams.toSingleValueMap().toString());
        log.info("URI: " + uri.toString()); 

        ResponseEntity<String> initialResponse = webClient.get()
            .uri(uri)
            .headers(httpHeaders -> {
                httpHeaders.addAll(headers);
                if (this.username != null && this.password != null) {
                    httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                }
            })
                //.retrieve()
                //.toBodilessEntity()
            .exchangeToMono(clientResponse ->clientResponse.toEntity(String.class))
            .block();

        if (initialResponse.getStatusCode().is3xxRedirection()) {
            URI location = initialResponse.getHeaders().getLocation();
            if (location != null) {
                String redirectUrl = location.toString();
                log.info("Redirecting to: " + redirectUrl);

                this.dataBufferFlux = webClient.get()
                    .uri(redirectUrl)
                    .headers(httpHeaders -> {
                            httpHeaders.addAll(headers);
                        if (this.username != null && this.password != null) {
                            httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                        }
                    })
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);

                this.statusCode = initialResponse.getStatusCode().value();
            } else {
                log.error("Redirection location is null.");
            }
        } else {
            log.error("Unexpected status code: " + initialResponse.getStatusCode());
        }
    }

}
