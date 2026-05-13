package com.detoxmate.discord;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

class DiscordWebhookClientTest {

    @Test
    void webhook_url이_설정되면_Discord로_메시지를_POST한다() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        DiscordWebhookClient client = new DiscordWebhookClient(restClientBuilder.build());

        server.expect(requestTo("https://discord.example/webhook"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "content": "OCR 오류 신고가 생성되었습니다.",
                          "embeds": [
                            {
                              "title": "Screen Time OCR Error Report #555",
                              "fields": [
                                {
                                  "name": "OCR 추측 시간",
                                  "value": "180분 (3시간 0분)",
                                  "inline": false
                                }
                              ]
                            }
                          ]
                        }
                        """))
                .andRespond(withNoContent());

        client.send("https://discord.example/webhook", new DiscordWebhookMessage(
                "OCR 오류 신고가 생성되었습니다.",
                List.of(new DiscordWebhookMessage.Embed(
                        "Screen Time OCR Error Report #555",
                        null,
                        null,
                        List.of(new DiscordWebhookMessage.Field("OCR 추측 시간", "180분 (3시간 0분)", false))
                ))
        ));

        server.verify();
    }

    @Test
    void webhook_url이_비어_있으면_HTTP_호출을_하지_않는다() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        DiscordWebhookClient client = new DiscordWebhookClient(restClientBuilder.build());

        server.expect(never(), requestTo("https://discord.example/webhook"));

        client.send("", new DiscordWebhookMessage("content", List.of()));

        server.verify();
    }
}
