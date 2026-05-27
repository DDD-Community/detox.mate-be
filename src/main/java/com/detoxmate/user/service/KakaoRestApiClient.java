package com.detoxmate.user.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.detoxmate.user.config.KakaoAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class KakaoRestApiClient {

    private final RestClient restClient;
    private final KakaoAuthProperties properties;

    @Autowired
    public KakaoRestApiClient(KakaoAuthProperties properties) {
        this(
                RestClient.builder()
                        .baseUrl("https://kapi.kakao.com")
                        .build(),
                properties
        );
    }

    KakaoRestApiClient(RestClient restClient, KakaoAuthProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public KakaoUserInfo getUserInfo(String providerAccessToken) {
        KakaoUserMeResponse response;

        try {
            response = restClient.get()
                    .uri("/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerAccessToken)
                    .retrieve()
                    .body(KakaoUserMeResponse.class);
        } catch (RestClientResponseException exception) {
            throw convertResponseException(exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao API request failed", exception);
        }

        if (response == null || response.id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao user info response is invalid");
        }

        return new KakaoUserInfo(
                String.valueOf(response.id()),
                extractNickname(response)
        );
    }

    public void unlinkByAdminKey(String providerUserId) {
        if (properties.adminKey() == null || properties.adminKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kakao admin key is not configured");
        }

        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("target_id_type", "user_id");
        request.add("target_id", providerUserId);

        KakaoUnlinkResponse response;

        try {
            response = restClient.post()
                    .uri("/v1/user/unlink")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.adminKey())
                    .body(request)
                    .retrieve()
                    .body(KakaoUnlinkResponse.class);
        } catch (RestClientResponseException exception) {
            throw convertResponseException(exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao API request failed", exception);
        }

        if (response == null || response.id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao unlink response is invalid");
        }
    }

    private ResponseStatusException convertResponseException(RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();

        if (statusCode == HttpStatus.BAD_REQUEST.value()) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Kakao access token request", exception);
        }

        if (statusCode == HttpStatus.UNAUTHORIZED.value() || statusCode == HttpStatus.FORBIDDEN.value()) {
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized Kakao access token", exception);
        }

        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao API request failed", exception);
    }

    private String extractNickname(KakaoUserMeResponse response) {
        if (response.kakaoAccount() == null) {
            return null;
        }

        if (response.kakaoAccount().name() != null) {
            return response.kakaoAccount().name();
        }

        if (response.kakaoAccount().profile() != null) {
            return response.kakaoAccount().profile().nickname();
        }

        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoUserMeResponse(
            Long id,
            KakaoAccount kakao_account
    ) {
        private KakaoAccount kakaoAccount() {
            return kakao_account;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoAccount(
            String name,
            KakaoProfile profile
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoProfile(
            String nickname
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoUnlinkResponse(
            Long id
    ) {
    }
}
