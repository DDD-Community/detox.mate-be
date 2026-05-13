package com.detoxmate.screentimeocr.discord;

import com.detoxmate.discord.DiscordWebhookClient;
import com.detoxmate.discord.DiscordWebhookMessage;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportDiscordNotificationRow;
import com.detoxmate.screentimeocr.repository.ScreenTimeOcrErrorReportRepository;
import com.detoxmate.upload.config.StorageProperties;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScreenTimeOcrDiscordNotifierTest {

    private final ScreenTimeOcrErrorReportRepository reportRepository = mock(ScreenTimeOcrErrorReportRepository.class);
    private final DiscordWebhookClient discordWebhookClient = mock(DiscordWebhookClient.class);
    private final ImageReadUrlBuilder imageReadUrlBuilder =
            new ImageReadUrlBuilder(new StorageProperties("https://example.com/media"));
    private final ScreenTimeOcrDiscordMessageFactory messageFactory = new ScreenTimeOcrDiscordMessageFactory();

    @Test
    void 설정된_webhook으로_OCR_오류_신고_알림을_보낸다() {
        ScreenTimeOcrDiscordNotifier notifier = notifier(
                new ScreenTimeOcrDiscordProperties(
                        "https://discord.example/webhook",
                        "https://api.detoxmate.example"
                )
        );
        when(reportRepository.findDiscordNotificationRowById(555L)).thenReturn(Optional.of(report()));

        notifier.send(555L);

        ArgumentCaptor<DiscordWebhookMessage> messageCaptor = ArgumentCaptor.forClass(DiscordWebhookMessage.class);
        verify(discordWebhookClient).send(eq("https://discord.example/webhook"), messageCaptor.capture());
        DiscordWebhookMessage message = messageCaptor.getValue();
        assertThat(message.embeds().getFirst().image().url())
                .isEqualTo("https://example.com/media/screen-time-ocr-reports/1/2026/05/sample.png");
        assertThat(message.embeds().getFirst().fields())
                .extracting(DiscordWebhookMessage.Field::value)
                .anySatisfy(value -> assertThat(value)
                        .contains("https://api.detoxmate.example/admin/screen-time-ocr-error-reports/555")
                        .contains("$ADMIN_REVIEW_TOKEN"));
    }

    @Test
    void webhook_url이_없으면_report를_조회하지_않는다() {
        ScreenTimeOcrDiscordNotifier notifier = notifier(new ScreenTimeOcrDiscordProperties("", ""));

        notifier.send(555L);

        verifyNoInteractions(reportRepository, discordWebhookClient);
    }

    private ScreenTimeOcrDiscordNotifier notifier(ScreenTimeOcrDiscordProperties properties) {
        return new ScreenTimeOcrDiscordNotifier(
                reportRepository,
                discordWebhookClient,
                imageReadUrlBuilder,
                messageFactory,
                properties
        );
    }

    private ScreenTimeOcrErrorReportDiscordNotificationRow report() {
        return new ScreenTimeOcrErrorReportDiscordNotificationRow(
                555L,
                1L,
                "지민",
                123L,
                10L,
                LocalDate.of(2026, 5, 12),
                "screen-time-ocr-reports/1/2026/05/sample.png",
                180
        );
    }
}
