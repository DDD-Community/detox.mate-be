package com.detoxmate.user.service;

import com.detoxmate.user.config.KakaoAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class KakaoRestApiClientTest {

    @Test
    void 카카오_유저정보_API를_호출하고_응답을_파싱한다() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://kapi.kakao.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        KakaoRestApiClient kakaoRestApiClient = new KakaoRestApiClient(
                restClientBuilder.build(),
                new KakaoAuthProperties("admin-key")
        );

        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-access-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 123456789,
                          "kakao_account": {
                            "profile": {
                              "nickname": "카카오닉네임"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        KakaoUserInfo userInfo = kakaoRestApiClient.getUserInfo("kakao-access-token");

        // then
        assertThat(userInfo.providerUserId()).isEqualTo("123456789");
        assertThat(userInfo.nickname()).isEqualTo("카카오닉네임");
        server.verify();
    }

    @Test
    void 카카오_401_응답이면_401_예외로_변환한다() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://kapi.kakao.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        KakaoRestApiClient kakaoRestApiClient = new KakaoRestApiClient(
                restClientBuilder.build(),
                new KakaoAuthProperties("admin-key")
        );

        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // when & then
        assertThatThrownBy(() -> kakaoRestApiClient.getUserInfo("invalid-kakao-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void 카카오_5xx_응답이면_502_예외로_변환한다() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://kapi.kakao.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        KakaoRestApiClient kakaoRestApiClient = new KakaoRestApiClient(
                restClientBuilder.build(),
                new KakaoAuthProperties("admin-key")
        );

        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> kakaoRestApiClient.getUserInfo("kakao-access-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(HttpStatus.BAD_GATEWAY.value());
    }

    @Test
    void 카카오_Admin_key로_연결을_해제한다() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://kapi.kakao.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        KakaoRestApiClient kakaoRestApiClient = new KakaoRestApiClient(
                restClientBuilder.build(),
                new KakaoAuthProperties("admin-key")
        );

        server.expect(requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK admin-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(content().string(containsString("target_id_type=user_id")))
                .andExpect(content().string(containsString("target_id=123456789")))
                .andRespond(withSuccess("""
                        {
                          "id": 123456789
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        kakaoRestApiClient.unlinkByAdminKey("123456789");

        // then
        server.verify();
    }
}
