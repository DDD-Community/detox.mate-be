package com.detoxmate.user.service;

import com.detoxmate.user.config.AppleAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class AppleRestApiClient {

    private final RestClient restClient;
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final AppleAuthProperties properties;

    @Autowired
    public AppleRestApiClient(AppleClientSecretGenerator clientSecretGenerator, AppleAuthProperties properties) {
        this(
                RestClient.builder()
                        .baseUrl("https://appleid.apple.com")
                        .build(),
                clientSecretGenerator,
                properties
        );
    }

    AppleRestApiClient(
            RestClient restClient,
            AppleClientSecretGenerator clientSecretGenerator,
            AppleAuthProperties properties
    ) {
        this.restClient = restClient;
        this.clientSecretGenerator = clientSecretGenerator;
        this.properties = properties;
    }

    public String exchangeAuthorizationCode(String authorizationCode) {
        AppleTokenResponse response;

        try {
            response = restClient.post()
                    .uri("/auth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(tokenRequest(authorizationCode))
                    .retrieve()
                    .body(AppleTokenResponse.class);
        } catch (RestClientResponseException exception) {
            throw convertTokenException(exception);
        } catch (RestClientException exception) {
            log.warn("[Auth][Apple] token request failed before response.", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apple token request failed", exception);
        }

        if (response == null || response.refreshToken() == null || response.refreshToken().isBlank()) {
            log.warn("[Auth][Apple] token response did not contain refresh token. responsePresent={}", response != null);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apple token response is invalid");
        }

        return response.refreshToken();
    }

    public void revokeRefreshToken(String refreshToken) {
        try {
            restClient.post()
                    .uri("/auth/revoke")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(revokeRequest(refreshToken))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.warn(
                    "[Auth][Apple] revoke request rejected. appleStatus={}, appleBody={}",
                    exception.getStatusCode().value(),
                    exception.getResponseBodyAsString(),
                    exception
            );
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apple revoke request failed", exception);
        } catch (RestClientException exception) {
            log.warn("[Auth][Apple] revoke request failed before response.", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apple revoke request failed", exception);
        }
    }

    private MultiValueMap<String, String> tokenRequest(String authorizationCode) {
        MultiValueMap<String, String> request = baseRequest();
        request.add("grant_type", "authorization_code");
        request.add("code", authorizationCode);
        return request;
    }

    private MultiValueMap<String, String> revokeRequest(String refreshToken) {
        MultiValueMap<String, String> request = baseRequest();
        request.add("token", refreshToken);
        request.add("token_type_hint", "refresh_token");
        return request;
    }

    private MultiValueMap<String, String> baseRequest() {
        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("client_id", requiredClientId());
        request.add("client_secret", clientSecretGenerator.generate());
        return request;
    }

    private String requiredClientId() {
        if (properties.clientId() == null || properties.clientId().isBlank()) {
            log.error("[Auth][Apple] clientId is not configured.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Apple clientId is not configured");
        }

        return properties.clientId();
    }

    private ResponseStatusException convertTokenException(RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        log.warn(
                "[Auth][Apple] token request rejected. appleStatus={}, appleBody={}",
                statusCode,
                exception.getResponseBodyAsString(),
                exception
        );
        if (statusCode == HttpStatus.BAD_REQUEST.value() || statusCode == HttpStatus.UNAUTHORIZED.value()) {
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Apple authorization code is invalid", exception);
        }

        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apple token request failed", exception);
    }
}
