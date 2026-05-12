package com.detoxmate.screentimeocr.discord;

import com.detoxmate.discord.DiscordWebhookClient;
import com.detoxmate.discord.DiscordWebhookMessage;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportDiscordNotificationRow;
import com.detoxmate.screentimeocr.repository.ScreenTimeOcrErrorReportRepository;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenTimeOcrDiscordNotifier {

    private final ScreenTimeOcrErrorReportRepository reportRepository;
    private final DiscordWebhookClient discordWebhookClient;
    private final ImageReadUrlBuilder imageReadUrlBuilder;
    private final ScreenTimeOcrDiscordMessageFactory messageFactory;
    private final ScreenTimeOcrDiscordProperties properties;

    @Transactional(readOnly = true)
    public void send(Long reportId) {
        if (!properties.hasWebhookUrl()) {
            return;
        }

        reportRepository.findDiscordNotificationRowById(reportId)
                .ifPresentOrElse(
                        this::send,
                        () -> log.warn("Screen time OCR report not found for Discord notification. reportId={}", reportId)
                );
    }

    private void send(ScreenTimeOcrErrorReportDiscordNotificationRow report) {
        String imageUrl = imageReadUrlBuilder.build(report.imageObjectKey());
        DiscordWebhookMessage message = messageFactory.create(
                report,
                imageUrl,
                properties.apiBaseUrlOrEnvReference()
        );
        discordWebhookClient.send(properties.webhookUrl(), message);
    }
}
