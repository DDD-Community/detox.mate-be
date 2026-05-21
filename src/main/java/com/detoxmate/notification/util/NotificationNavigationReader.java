package com.detoxmate.notification.util;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.repository.CommentRepository;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationNavigationReader {

    private final ChallengeRecordRepository challengeRecordRepository;
    private final CommentRepository commentRepository;
    private final GroupChallengeParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public Optional<ChallengeRecord> findChallengeRecord(Long challengeRecordId) {
        return challengeRecordRepository.findById(challengeRecordId);
    }

    @Transactional(readOnly = true)
    public Optional<Comment> findComment(Long commentId) {
        return commentRepository.findById(commentId);
    }

    @Transactional(readOnly = true)
    public boolean canAccessFeed(Long userId, Long groupChallengeId) {
        return participantRepository.existsByGroupChallengeIdAndUserId(groupChallengeId, userId);
    }
}
