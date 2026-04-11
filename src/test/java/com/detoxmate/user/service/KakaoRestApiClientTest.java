package com.detoxmate.user.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoRestApiClientTest {

    @Test
    void 카카오_유저정보_API를_호출하고_응답을_파싱한다() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://kapi.kakao.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        KakaoRestApiClient kakaoRestApiClient = new KakaoRestApiClient(restClientBuilder.build());

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
}
