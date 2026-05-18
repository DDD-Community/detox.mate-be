package com.detoxmate.screentimeocr.discord;

import com.detoxmate.discord.DiscordWebhookMessage;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportDiscordNotificationRow;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScreenTimeOcrDiscordMessageFactory {

    private static final String ADMIN_TOKEN_PLACEHOLDER = "$ADMIN_REVIEW_TOKEN";

    public DiscordWebhookMessage create(
            ScreenTimeOcrErrorReportDiscordNotificationRow report,
            String imageUrl,
            String apiBaseUrl
    ) {
        return new DiscordWebhookMessage(
                "OCR 오류 신고가 생성되었습니다.",
                List.of(new DiscordWebhookMessage.Embed(
                        "Screen Time OCR Error Report #%d".formatted(report.id()),
                        description(report),
                        new DiscordWebhookMessage.Image(imageUrl),
                        fields(report, normalizedApiBaseUrl(apiBaseUrl))
                ))
        );
    }

    private String description(ScreenTimeOcrErrorReportDiscordNotificationRow report) {
        return """
                User: %s (userId=%d)
                Record Date: %s
                ActivityRecord: %s
                Participant: %d
                """.formatted(
                report.userDisplayName(),
                report.userId(),
                report.recordDate(),
                nullableId(report.activityRecordId()),
                report.groupChallengeParticipantId()
        );
    }

    private List<DiscordWebhookMessage.Field> fields(
            ScreenTimeOcrErrorReportDiscordNotificationRow report,
            String apiBaseUrl
    ) {
        return List.of(
                new DiscordWebhookMessage.Field(
                        "OCR 추측 시간",
                        formattedMinutes(report.ocrTotalUsedMinutes()),
                        false
                ),
                new DiscordWebhookMessage.Field(
                        "수정 curl",
                        codeBlock(correctCurl(report.id(), apiBaseUrl)),
                        false
                ),
                new DiscordWebhookMessage.Field(
                        "반려 curl",
                        codeBlock(rejectCurl(report.id(), apiBaseUrl)),
                        false
                )
        );
    }

    private String correctCurl(Long reportId, String apiBaseUrl) {
        return """
                curl -X PATCH "%s/admin/screen-time-ocr-error-reports/%d" \\
                  -H "X-Admin-Token: %s" \\
                  -H "Content-Type: application/json" \\
                  -d '{"action":"CORRECT","correctedTotalUsedMinutes":165,"adminNote":"스크린샷 기준 총 사용시간 2시간 45분"}'
                """.formatted(apiBaseUrl, reportId, ADMIN_TOKEN_PLACEHOLDER).strip();
    }

    private String rejectCurl(Long reportId, String apiBaseUrl) {
        return """
                curl -X PATCH "%s/admin/screen-time-ocr-error-reports/%d" \\
                  -H "X-Admin-Token: %s" \\
                  -H "Content-Type: application/json" \\
                  -d '{"action":"REJECT","adminNote":"OCR 오류를 확인할 수 없음"}'
                """.formatted(apiBaseUrl, reportId, ADMIN_TOKEN_PLACEHOLDER).strip();
    }

    private String formattedMinutes(Integer minutes) {
        int safeMinutes = minutes == null ? 0 : minutes;
        return "%d분 (%d시간 %d분)".formatted(safeMinutes, safeMinutes / 60, safeMinutes % 60);
    }

    private String nullableId(Long id) {
        if (id == null) {
            return "없음";
        }
        return String.valueOf(id);
    }

    private String normalizedApiBaseUrl(String apiBaseUrl) {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            return "$APP_PUBLIC_BASE_URL";
        }

        int end = apiBaseUrl.length();
        while (end > 0 && apiBaseUrl.charAt(end - 1) == '/') {
            end--;
        }
        return apiBaseUrl.substring(0, end);
    }

    private String codeBlock(String value) {
        return "```bash\n%s\n```".formatted(value);
    }
}
