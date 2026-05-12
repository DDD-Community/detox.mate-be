package com.detoxmate.screentimeocr.domain;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "screen_time_ocr_error_report",
        indexes = {
                @Index(
                        name = "idx_screen_time_ocr_report_status_created_at",
                        columnList = "status, created_at"
                ),
                @Index(
                        name = "idx_screen_time_ocr_report_user_created_at",
                        columnList = "user_id, created_at"
                ),
                @Index(
                        name = "idx_screen_time_ocr_report_activity_record",
                        columnList = "activity_record_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScreenTimeOcrErrorReport {

    @Id
    @Column(name = "screen_time_ocr_error_report_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_record_id")
    private ActivityRecord activityRecord;

    @Column(name = "group_challenge_participant_id")
    private Long groupChallengeParticipantId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "image_object_key", nullable = false, length = 1024)
    private String imageObjectKey;

    @Column(name = "ocr_total_used_minutes", nullable = false)
    private Integer ocrTotalUsedMinutes;

    @Column(name = "corrected_total_used_minutes")
    private Integer correctedTotalUsedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ScreenTimeOcrErrorReportStatus status;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ScreenTimeOcrErrorReport(
            User user,
            ActivityRecord activityRecord,
            Long groupChallengeParticipantId,
            LocalDate recordDate,
            String imageObjectKey,
            Integer ocrTotalUsedMinutes
    ) {
        validateUser(user);
        validateRecordDate(recordDate);
        validateImageObjectKey(imageObjectKey);
        validateMinutes(ocrTotalUsedMinutes);
        this.user = user;
        this.activityRecord = activityRecord;
        this.groupChallengeParticipantId = groupChallengeParticipantId;
        this.recordDate = recordDate;
        this.imageObjectKey = imageObjectKey;
        this.ocrTotalUsedMinutes = ocrTotalUsedMinutes;
        this.status = ScreenTimeOcrErrorReportStatus.PENDING;
    }

    public static ScreenTimeOcrErrorReport create(
            User user,
            ActivityRecord activityRecord,
            Long groupChallengeParticipantId,
            LocalDate recordDate,
            String imageObjectKey,
            Integer ocrTotalUsedMinutes
    ) {
        return new ScreenTimeOcrErrorReport(
                user,
                activityRecord,
                groupChallengeParticipantId,
                recordDate,
                imageObjectKey,
                ocrTotalUsedMinutes
        );
    }

    public Long getUserId() {
        return user.getId();
    }

    public Long getActivityRecordId() {
        if (activityRecord == null) {
            return null;
        }
        return activityRecord.getId();
    }

    public boolean isPending() {
        return status == ScreenTimeOcrErrorReportStatus.PENDING;
    }

    public boolean hasActivityRecord() {
        return activityRecord != null;
    }

    public void markCorrected(Integer correctedTotalUsedMinutes, Long resolvedByUserId, LocalDateTime resolvedAt, String adminNote) {
        validateMinutes(correctedTotalUsedMinutes);
        validateResolvedByUserId(resolvedByUserId);
        validateResolvedAt(resolvedAt);
        this.correctedTotalUsedMinutes = correctedTotalUsedMinutes;
        this.status = ScreenTimeOcrErrorReportStatus.CORRECTED;
        this.resolvedByUserId = resolvedByUserId;
        this.resolvedAt = resolvedAt;
        this.adminNote = adminNote;
    }

    public void reject(Long resolvedByUserId, LocalDateTime resolvedAt, String adminNote) {
        validateResolvedByUserId(resolvedByUserId);
        validateResolvedAt(resolvedAt);
        this.status = ScreenTimeOcrErrorReportStatus.REJECTED;
        this.resolvedByUserId = resolvedByUserId;
        this.resolvedAt = resolvedAt;
        this.adminNote = adminNote;
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user 는 필수입니다.");
        }
    }

    private static void validateRecordDate(LocalDate recordDate) {
        if (recordDate == null) {
            throw new IllegalArgumentException("recordDate 는 필수입니다.");
        }
    }

    private static void validateImageObjectKey(String imageObjectKey) {
        if (imageObjectKey == null || imageObjectKey.isBlank()) {
            throw new IllegalArgumentException("imageObjectKey 는 필수입니다.");
        }
    }

    private static void validateMinutes(Integer minutes) {
        if (minutes == null) {
            throw new IllegalArgumentException("minutes 는 필수입니다.");
        }

        if (minutes < 0) {
            throw new IllegalArgumentException("minutes 는 0 이상이어야 합니다.");
        }
    }

    private static void validateResolvedByUserId(Long resolvedByUserId) {
        if (resolvedByUserId == null) {
            throw new IllegalArgumentException("resolvedByUserId 는 필수입니다.");
        }
    }

    private static void validateResolvedAt(LocalDateTime resolvedAt) {
        if (resolvedAt == null) {
            throw new IllegalArgumentException("resolvedAt 는 필수입니다.");
        }
    }
}
