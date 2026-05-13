package com.detoxmate.screentimeocr.domain;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.group.domain.GroupChallengeParticipant;
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
                        columnList = "status, created_at, screen_time_ocr_error_report_id"
                ),
                @Index(
                        name = "idx_screen_time_ocr_report_participant_record_date",
                        columnList = "group_challenge_participant_id, record_date"
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_record_id")
    private ActivityRecord activityRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_challenge_participant_id", nullable = false)
    private GroupChallengeParticipant groupChallengeParticipant;

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

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ScreenTimeOcrErrorReport(
            GroupChallengeParticipant groupChallengeParticipant,
            ActivityRecord activityRecord,
            LocalDate recordDate,
            String imageObjectKey,
            Integer ocrTotalUsedMinutes
    ) {
        validateGroupChallengeParticipant(groupChallengeParticipant);
        validateRecordDate(recordDate);
        validateImageObjectKey(imageObjectKey);
        validateMinutes(ocrTotalUsedMinutes);
        this.groupChallengeParticipant = groupChallengeParticipant;
        this.activityRecord = activityRecord;
        this.recordDate = recordDate;
        this.imageObjectKey = imageObjectKey;
        this.ocrTotalUsedMinutes = ocrTotalUsedMinutes;
        this.status = ScreenTimeOcrErrorReportStatus.PENDING;
    }

    public static ScreenTimeOcrErrorReport create(
            GroupChallengeParticipant groupChallengeParticipant,
            ActivityRecord activityRecord,
            LocalDate recordDate,
            String imageObjectKey,
            Integer ocrTotalUsedMinutes
    ) {
        return new ScreenTimeOcrErrorReport(
                groupChallengeParticipant,
                activityRecord,
                recordDate,
                imageObjectKey,
                ocrTotalUsedMinutes
        );
    }

    public Long getActivityRecordId() {
        if (activityRecord == null) {
            return null;
        }
        return activityRecord.getId();
    }

    public Long getGroupChallengeParticipantId() {
        return groupChallengeParticipant.getId();
    }

    public boolean isPending() {
        return status == ScreenTimeOcrErrorReportStatus.PENDING;
    }

    public boolean hasActivityRecord() {
        return activityRecord != null;
    }

    public void linkActivityRecord(ActivityRecord activityRecord) {
        if (activityRecord == null) {
            throw new IllegalArgumentException("activityRecord 는 필수입니다.");
        }
        this.activityRecord = activityRecord;
    }

    public void markCorrected(Integer correctedTotalUsedMinutes, LocalDateTime resolvedAt, String adminNote) {
        validateMinutes(correctedTotalUsedMinutes);
        validateResolvedAt(resolvedAt);
        this.correctedTotalUsedMinutes = correctedTotalUsedMinutes;
        this.status = ScreenTimeOcrErrorReportStatus.CORRECTED;
        this.resolvedAt = resolvedAt;
        this.adminNote = adminNote;
    }

    public void reject(LocalDateTime resolvedAt, String adminNote) {
        validateResolvedAt(resolvedAt);
        this.status = ScreenTimeOcrErrorReportStatus.REJECTED;
        this.resolvedAt = resolvedAt;
        this.adminNote = adminNote;
    }

    private static void validateGroupChallengeParticipant(GroupChallengeParticipant groupChallengeParticipant) {
        if (groupChallengeParticipant == null) {
            throw new IllegalArgumentException("groupChallengeParticipant 는 필수입니다.");
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

    private static void validateResolvedAt(LocalDateTime resolvedAt) {
        if (resolvedAt == null) {
            throw new IllegalArgumentException("resolvedAt 는 필수입니다.");
        }
    }
}
