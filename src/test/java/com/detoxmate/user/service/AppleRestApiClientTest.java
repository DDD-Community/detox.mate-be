package com.detoxmate.user.service;

import com.detoxmate.user.config.AppleAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppleRestApiClientTest {

    @Test
    void Apple_authorization_code를_refresh_token으로_교환한다() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://appleid.apple.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AppleClientSecretGenerator clientSecretGenerator = mock(AppleClientSecretGenerator.class);
        AppleRestApiClient appleRestApiClient = new AppleRestApiClient(
                restClientBuilder.build(),
                clientSecretGenerator,
                new AppleAuthProperties("apple-client-id", "team-id", "key-id", "private-key")
        );

        when(clientSecretGenerator.generate()).thenReturn("apple-client-secret");
        server.expect(requestTo("https://appleid.apple.com/auth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(content().string(containsString("client_id=apple-client-id")))
                .andExpect(content().string(containsString("client_secret=apple-client-secret")))
                .andExpect(content().string(containsString("grant_type=authorization_code")))
                .andExpect(content().string(containsString("code=apple-authorization-code")))
                .andRespond(withSuccess("""
                        {
                          "refresh_token": "apple-refresh-token"
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        String refreshToken = appleRestApiClient.exchangeAuthorizationCode("apple-authorization-code");

        // then
        assertThat(refreshToken).isEqualTo("apple-refresh-token");
        server.verify();
    }

    @Test
    void Apple_refresh_token을_revoke한다() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://appleid.apple.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AppleClientSecretGenerator clientSecretGenerator = mock(AppleClientSecretGenerator.class);
        AppleRestApiClient appleRestApiClient = new AppleRestApiClient(
                restClientBuilder.build(),
                clientSecretGenerator,
                new AppleAuthProperties("apple-client-id", "team-id", "key-id", "private-key")
        );

        when(clientSecretGenerator.generate()).thenReturn("apple-client-secret");
        server.expect(requestTo("https://appleid.apple.com/auth/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(content().string(containsString("client_id=apple-client-id")))
                .andExpect(content().string(containsString("client_secret=apple-client-secret")))
                .andExpect(content().string(containsString("token=apple-refresh-token")))
                .andExpect(content().string(containsString("token_type_hint=refresh_token")))
                .andRespond(withSuccess());

        // when
        appleRestApiClient.revokeRefreshToken("apple-refresh-token");

        // then
        server.verify();
    }
}
