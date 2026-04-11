package com.detoxmate.user.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoRestApiClient {

    private final RestClient restClient;

    public KakaoRestApiClient() {
        this(RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build());
    }

    KakaoRestApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public KakaoUserInfo getUserInfo(String providerAccessToken) {
        KakaoUserMeResponse response = restClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerAccessToken)
                .retrieve()
                .body(KakaoUserMeResponse.class);

        if (response == null || response.id() == null) {
            throw new IllegalStateException("Kakao user info response is invalid");
        }

        return new KakaoUserInfo(
                String.valueOf(response.id()),
                extractNickname(response)
        );
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
}
