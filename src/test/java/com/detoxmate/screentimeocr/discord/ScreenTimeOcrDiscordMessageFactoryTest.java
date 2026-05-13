package com.detoxmate.screentimeocr.discord;

import com.detoxmate.discord.DiscordWebhookMessage;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportDiscordNotificationRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ScreenTimeOcrDiscordMessageFactoryTest {

    private final ScreenTimeOcrDiscordMessageFactory messageFactory = new ScreenTimeOcrDiscordMessageFactory();

    @Test
    void OCR_오류_신고_검수에_필요한_Discord_메시지를_만든다() {
        ScreenTimeOcrErrorReportDiscordNotificationRow report =
                new ScreenTimeOcrErrorReportDiscordNotificationRow(
                        555L,
                        1L,
                        "지민",
                        123L,
                        10L,
                        LocalDate.of(2026, 5, 12),
                        "screen-time-ocr-reports/1/2026/05/sample.png",
                        180
                );

        DiscordWebhookMessage message = messageFactory.create(
                report,
                "https://example.com/media/screen-time-ocr-reports/1/2026/05/sample.png",
                "https://api.detoxmate.example"
        );

        assertThat(message.content()).contains("OCR 오류 신고");
        assertThat(message.embeds()).hasSize(1);
        DiscordWebhookMessage.Embed embed = message.embeds().getFirst();
        assertThat(embed.title()).isEqualTo("Screen Time OCR Error Report #555");
        assertThat(embed.image().url()).isEqualTo("https://example.com/media/screen-time-ocr-reports/1/2026/05/sample.png");
        assertThat(embed.fields())
                .extracting(DiscordWebhookMessage.Field::value)
                .anySatisfy(value -> assertThat(value).contains("180분", "3시간 0분"))
                .anySatisfy(value -> assertThat(value)
                        .contains("curl -X PATCH")
                        .contains("https://api.detoxmate.example/admin/screen-time-ocr-error-reports/555")
                        .contains("\"action\":\"CORRECT\"")
                        .contains("$ADMIN_REVIEW_TOKEN")
                        .doesNotContain("test-admin-token"))
                .anySatisfy(value -> assertThat(value).contains("\"action\":\"REJECT\""));
    }
}
