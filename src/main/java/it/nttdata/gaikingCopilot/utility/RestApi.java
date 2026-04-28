package it.nttdata.gaikingCopilot.utility;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;


import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;


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

    public Mono<ResponseEntity<String>> requestPost(){
        WebClient webClient = this.configureWebClient();
        return webClient.post()
                    .uri(this.url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(this.payload)
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class));

    }

    public Mono<ResponseEntity<String>> requestGet(){

        WebClient webClient = this.configureWebClient();
        return webClient.get()
                .uri(this.url)
                .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class));

    }

    public Mono<ResponseEntity<String>> requestPostWithHeadersWithoutParameters() {
        WebClient webClient = this.configureWebClient();
        return webClient.post()
                    .uri(this.url)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .bodyValue(this.payload)
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class));
    }

    public Mono<ResponseEntity<String>> requestPostWithHeadersAndParameters(){
        WebClient webClient = this.configureWebClient();
        return webClient.post()
                    .uri(UriComponentsBuilder.fromUriString(this.url).queryParams(this.queryParams).build().toUri())
                    .headers(httpHeaders ->{ 
                        httpHeaders.addAll(headers); 
                        if(this.username != null && this.password != null) {
                            httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                        }
                    })
                    .bodyValue(this.payload)
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class));

    }

    public Mono<ResponseEntity<String>> requestPostUrlEncoded(){
        WebClient webClient = this.configureWebClient();
        return webClient.post()
                    .uri(UriComponentsBuilder.fromUriString(this.url).queryParams(this.queryParams).build().toUri())
                    .headers(httpHeaders ->{ 
                        httpHeaders.addAll(headers); 
                        if(this.username != null && this.password != null) {
                            httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                        }
                    })
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(this.payloadMap))
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class));

    }

    public Mono<ResponseEntity<String>> requestGetWithHeadersAndParameters(){
        WebClient webClient = this.configureWebClient();
        return webClient.get()
                .uri(UriComponentsBuilder.fromUriString(this.url).queryParams(this.queryParams).build().toUri())
                .headers(httpHeaders ->{ 
                        httpHeaders.addAll(headers); 
                        if(this.username != null && this.password != null) {
                            httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                        }
                    })
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class));
    }

    public Mono<ResponseEntity<String>> requestPatchWithHeadersAndParameters(){
        WebClient webClient = this.configureWebClient();
            return webClient.patch()
                    .uri(UriComponentsBuilder.fromUriString(this.url).queryParams(this.queryParams).build().toUri())
                    .headers(httpHeaders ->{ 
                        httpHeaders.addAll(headers); 
                        if(this.username != null && this.password != null) {
                            httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                        }
                    })
                    .bodyValue(this.payload)
                    .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class));

    }

    public Mono<ResponseEntity<String>> requestDeleteWithHeadersAndParameters(){
        WebClient webClient = this.configureWebClient();
        URI uri = queryParams != null
                ? UriComponentsBuilder.fromUriString(this.url).queryParams(this.queryParams).build().toUri()
                : UriComponentsBuilder.fromUriString(this.url).build().toUri();

        return webClient.method(HttpMethod.DELETE)
                .uri(uri)
                .headers(httpHeaders -> {
                    if (headers != null) {
                        httpHeaders.addAll(headers);
                    }
                    if (this.username != null && this.password != null) {
                        httpHeaders.add(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
                    }
                })
                .bodyValue(this.payload)
                .exchangeToMono(webClientResponse -> webClientResponse.toEntity(String.class));

    }
}
