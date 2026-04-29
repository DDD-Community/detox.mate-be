package com.detoxmate.comment.service;

import com.detoxmate.activityrecordchallengestatus.service.ActivityRecordChallengeStatusService;
import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.dto.request.CreateCommentRequest;
import com.detoxmate.comment.dto.response.CommentAuthorInfo;
import com.detoxmate.comment.dto.response.CommentItem;
import com.detoxmate.comment.dto.response.CommentListResponse;
import com.detoxmate.comment.dto.response.CommentResponse;
import com.detoxmate.comment.repository.CommentRepository;
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
    private final UserService userService;
    private final ActivityRecordChallengeStatusService statusService;

    @Transactional(readOnly = true)
    public CommentListResponse list(Long groupChallengeId, Long activityRecordId, String cursor, int size) {
        // size + 1 로 조회해서 다음 페이지 유무 판별
        Pageable pageable = Pageable.ofSize(size + 1);

        //cursor 유무에 따라 첫 페이지 / 다음 페이지 조회
        List<Comment> fetched = fetchComments(groupChallengeId, activityRecordId, cursor, pageable);

        //size보다 많이 조회됐으면 다음 페이지 있음
        boolean hasMore = fetched.size() > size;
        List<Comment> page = hasMore ? fetched.subList(0, size) : fetched;

        //nextCursor 계산
        String nextCursor = resolveNextCursor(page, hasMore);
        //전체 댓글 수 조회
        long totalCount = commentRepository.countByGroupChallengeIdAndActivityRecordId(
                groupChallengeId,
                activityRecordId
        );
        //댓글 작성자 프로필 조회
        Map<Long, MyProfileResponse> profiles = findProfiles(page);

        //CommentItem 응답 DTO로 변환
        List<CommentItem> items = page.stream().map(comment -> toCommentItem(comment, profiles.get(comment.getUserId())))
                .toList();

        return new CommentListResponse(totalCount, items, nextCursor);
    }

    @Transactional
    public CommentResponse create(Long groupChallengeId, Long activityRecordId, CreateCommentRequest request, Long currentUserId) {
        Comment comment = Comment.create(activityRecordId, groupChallengeId, currentUserId, request.commentBody());
        Comment saved = commentRepository.save(comment);

        statusService.increaseCommentCount(groupChallengeId, activityRecordId);
        return toCommentResponse(saved);
    }

    private List<Comment> fetchComments(Long groupChallengeId, Long activityRecordId, String cursor, Pageable pageable) {
        if (cursor == null || cursor.isBlank()) {
            return commentRepository.findByGroupChallengeIdAndActivityRecordIdOrderByIdAsc(
                    groupChallengeId,
                    activityRecordId,
                    pageable
            );
        }

        Long cursorId = Long.parseLong(cursor);

        return commentRepository.findByGroupChallengeIdAndActivityRecordIdAndIdGreaterThanOrderByIdAsc(
                groupChallengeId,
                activityRecordId,
                cursorId,
                pageable
        );
    }

    private String resolveNextCursor(List<Comment> page, boolean hasMore) {
        if (!hasMore) {
            return null;
        }

        return String.valueOf(page.get(page.size() - 1).getId());
    }

    private Map<Long, MyProfileResponse> findProfiles(List<Comment> comments) {
        Set<Long> authorIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());

        return userService.getProfilesByIds(authorIds);
    }

    private CommentAuthorInfo toAuthorInfo(Comment comment, MyProfileResponse profile) {
        if (profile != null) {
            return new CommentAuthorInfo(
                    profile.id(),
                    profile.displayName(),
                    profile.profileImageUrl()
            );
        }

        return new CommentAuthorInfo(
                comment.getUserId(),
                null,
                null
        );
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getGroupChallengeId(),
                comment.getActivityRecordId(),
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
