package com.detoxmate.comment.service;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.challengerecordstatuscount.service.ChallengeRecordStatusCountService;
import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.domain.CommentStatus;
import com.detoxmate.comment.dto.request.CreateCommentRequest;
import com.detoxmate.comment.dto.response.CommentAuthorInfo;
import com.detoxmate.comment.dto.response.CommentItem;
import com.detoxmate.comment.dto.response.CommentListResponse;
import com.detoxmate.comment.dto.response.CommentResponse;
import com.detoxmate.comment.repository.CommentRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.challengerecord.ChallengeRecordErrorCode;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final ChallengeRecordService challengeRecordService;
    private final ChallengeRecordStatusCountService statusCountService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public CommentListResponse list(Long challengeRecordId, String cursor, int size) {
        ChallengeRecord challengeRecord = findChallengeRecord(challengeRecordId);
        CommentStatus commentStatus = resolveCommentStatus(challengeRecord);

        Pageable pageable = Pageable.ofSize(size + 1);

        List<Comment> fetched = fetchComments(challengeRecordId, commentStatus, cursor, pageable);

        boolean hasMore = fetched.size() > size;
        List<Comment> page = hasMore ? fetched.subList(0, size) : fetched;

        String nextCursor = resolveNextCursor(page, hasMore);

        long totalCount = commentRepository.countByChallengeRecord(challengeRecordId, commentStatus);

        Map<Long, MyProfileResponse> profiles = findProfiles(page);

        List<CommentItem> items = page.stream()
                .map(comment -> toCommentItem(comment, profiles.get(comment.getUserId())))
                .toList();

        return new CommentListResponse(totalCount, items, nextCursor);
    }

    @Transactional
    public CommentResponse create(Long challengeRecordId, CreateCommentRequest request, Long currentUserId) {
        ChallengeRecord challengeRecord = findChallengeRecord(challengeRecordId);
        CommentStatus commentStatus = resolveCommentStatus(challengeRecord);

        Comment comment = Comment.create(
                challengeRecordId,
                currentUserId,
                request.commentBody(),
                commentStatus
        );

        Comment saved = commentRepository.save(comment);

        increaseCommentCount(challengeRecordId, commentStatus);

        return toCommentResponse(saved);
    }


    private ChallengeRecord findChallengeRecord(Long challengeRecordId) {
        return challengeRecordService.get(challengeRecordId);
    }

    private CommentStatus resolveCommentStatus(ChallengeRecord challengeRecord) {
        if (challengeRecord.isCertified()) {
            return CommentStatus.AFTER_RECORD;
        }

        return CommentStatus.BEFORE_RECORD;
    }

    private List<Comment> fetchComments(
            Long challengeRecordId,
            CommentStatus commentStatus,
            String cursor,
            Pageable pageable
    ) {
        if (cursor == null || cursor.isBlank()) {
            return commentRepository.findByChallengeRecord(
                    challengeRecordId,
                    commentStatus,
                    pageable
            );
        }

        Long cursorId = Long.parseLong(cursor);

        return commentRepository.findByChallengeRecordAfterCursor(
                challengeRecordId,
                commentStatus,
                cursorId,
                pageable
        );
    }

    private void increaseCommentCount(Long challengeRecordId, CommentStatus commentStatus) {
        if (commentStatus == CommentStatus.BEFORE_RECORD) {
            statusCountService.increaseBeforeCommentCount(challengeRecordId);
            return;
        }

        statusCountService.increaseAfterCommentCount(challengeRecordId);
    }

    private String resolveNextCursor(List<Comment> page, boolean hasMore) {
        if (!hasMore) {
            return null;
        }

        return String.valueOf(page.get(page.size() - 1).getId());
    }

    private Map<Long, MyProfileResponse> findProfiles(List<Comment> comments) {
        Set<Long> authorIds = comments.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());

        return userService.getProfilesByIds(authorIds);
    }

    private CommentAuthorInfo toAuthorInfo(Comment comment, MyProfileResponse profile) {
        if (profile != null) {
            return new CommentAuthorInfo(profile.id(), profile.displayName(), profile.profileImageUrl());
        }

        return new CommentAuthorInfo(comment.getUserId(), null, null);
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getChallengeRecordId(),
                comment.getUserId(),
                comment.getCommentBody(),
                comment.getCreatedAt()
        );
    }

    private CommentItem toCommentItem(Comment comment, MyProfileResponse profile) {
        CommentAuthorInfo author = toAuthorInfo(comment, profile);

        return new CommentItem(
                comment.getId(),
                author,
                comment.getCommentBody(),
                comment.getCreatedAt()
        );
    }
}
