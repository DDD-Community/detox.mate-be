package com.detoxmate.notification.util;

import com.detoxmate.comment.repository.CommentRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.comment.CommentErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationCommentReader {

    private final CommentRepository commentRepository;

    public String findCommentBody(Long commentId){
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND))
                .getCommentBody();
    }
}
